package com.mhyddnn.mubarokappmobile.core.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject

/**
 * [CookieJar] sederhana namun persisten: menyimpan cookie (termasuk `access_token` &
 * `refresh_token` yang dikirim backend lewat `Set-Cookie`) di SharedPreferences sehingga
 * sesi bertahan setelah aplikasi ditutup. Cookie kedaluwarsa dibuang otomatis.
 *
 * Cukup untuk kebutuhan satu host (backend ERP). Tidak memakai dependency pihak ketiga.
 */
class SessionCookieJar(context: Context) : CookieJar {

    private val prefs =
        context.applicationContext.getSharedPreferences("session_cookies", Context.MODE_PRIVATE)

    // key: "name|domain|path" -> Cookie
    private val cache = LinkedHashMap<String, Cookie>()

    init {
        load()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            cache[keyOf(cookie)] = cookie
        }
        purgeExpired()
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        purgeExpired()
        return cache.values.filter { it.matches(url) }
    }

    /** Hapus semua cookie — dipanggil saat logout. */
    @Synchronized
    fun clear() {
        cache.clear()
        prefs.edit().clear().apply()
    }

    private fun keyOf(c: Cookie) = "${c.name}|${c.domain}|${c.path}"

    private fun purgeExpired() {
        val now = System.currentTimeMillis()
        val it = cache.entries.iterator()
        while (it.hasNext()) {
            if (it.next().value.expiresAt < now) it.remove()
        }
    }

    private fun persist() {
        val arr = JSONArray()
        for (c in cache.values) {
            arr.put(
                JSONObject().apply {
                    put("name", c.name)
                    put("value", c.value)
                    put("expiresAt", c.expiresAt)
                    put("domain", c.domain)
                    put("path", c.path)
                    put("secure", c.secure)
                    put("httpOnly", c.httpOnly)
                    put("hostOnly", c.hostOnly)
                },
            )
        }
        prefs.edit().putString("cookies", arr.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString("cookies", null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val builder = Cookie.Builder()
                    .name(o.getString("name"))
                    .value(o.getString("value"))
                    .expiresAt(o.getLong("expiresAt"))
                    .path(o.getString("path"))
                if (o.getBoolean("hostOnly")) {
                    builder.hostOnlyDomain(o.getString("domain"))
                } else {
                    builder.domain(o.getString("domain"))
                }
                if (o.getBoolean("secure")) builder.secure()
                if (o.getBoolean("httpOnly")) builder.httpOnly()
                cache[
                    "${o.getString("name")}|${o.getString("domain")}|${o.getString("path")}",
                ] = builder.build()
            }
            purgeExpired()
        }
    }
}
