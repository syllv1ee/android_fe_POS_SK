package com.mhyddnn.mubarokappmobile.core.network

import com.mhyddnn.mubarokappmobile.BuildConfig
import com.mhyddnn.mubarokappmobile.core.datastore.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor DEV: menukar scheme/host/port tiap request ke override URL server dari setting app
 * ([SettingsManager.serverBaseUrl]) — memungkinkan ganti IP tanpa rebuild. Path/query asli
 * (`/api/...`) dipertahankan. Aktif hanya di build DEBUG; release selalu pakai BASE_URL.
 *
 * Dipasang PERTAMA di kedua client (utama & refresh) agar interceptor berikutnya dan log melihat
 * URL akhir. `runBlocking` aman: berjalan di thread OkHttp, dan DataStore menyimpan nilai terakhir
 * di memori sehingga pembacaan setelah yang pertama tidak menyentuh disk.
 */
@Singleton
class ServerUrlInterceptor @Inject constructor(
    private val settingsManager: SettingsManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!BuildConfig.DEBUG) return chain.proceed(request)

        val override = runBlocking { settingsManager.serverBaseUrl.first() }
            ?.toHttpUrlOrNull()
            ?: return chain.proceed(request)

        val newUrl = request.url.newBuilder()
            .scheme(override.scheme)
            .host(override.host)
            .port(override.port)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
