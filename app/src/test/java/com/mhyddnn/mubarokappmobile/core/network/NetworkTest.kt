package com.mhyddnn.mubarokappmobile.core.network

import com.mhyddnn.mubarokappmobile.core.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import okhttp3.Response as OkResponse
import retrofit2.Response as RetrofitResponse

private class FakeTokenStore(
    override var accessToken: String? = null,
    override var refreshToken: String? = null,
) : TokenStore {
    override fun save(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override fun clear() {
        accessToken = null
        refreshToken = null
    }
}

class TokenAuthenticatorTest {

    /** "Server": refresh R1 → A2/R2 (rotasi); kode lain sesuai [code]. */
    private class FakeRefreshApi(var code: Int = 200, var throwIo: Boolean = false) : TokenRefreshApi {
        val sentTokens = mutableListOf<String>()
        override suspend fun refresh(body: RefreshRequest): RetrofitResponse<ApiResponse<TokenPair>> {
            sentTokens += body.refreshToken
            if (throwIo) throw IOException("offline")
            return if (code in 200..299) {
                RetrofitResponse.success(code, ApiResponse(success = true, data = TokenPair("A2", "R2")))
            } else {
                RetrofitResponse.error(code, "".toResponseBody())
            }
        }
    }

    private fun unauthorized(path: String = "plan", bearer: String? = "A1") =
        OkResponse.Builder()
            .request(
                Request.Builder().url("http://x/api/$path")
                    .apply { if (bearer != null) header("Authorization", "Bearer $bearer") }
                    .build(),
            )
            .protocol(Protocol.HTTP_1_1)
            .code(401).message("Unauthorized")
            .build()

    @Test
    fun refreshSuccess_savesRotatedTokens_andRetriesWithNewBearer() {
        val api = FakeRefreshApi(200)
        val tokens = FakeTokenStore("A1", "R1")
        var expired = false
        val retry = TokenAuthenticator(api, tokens) { expired = true }.authenticate(null, unauthorized())
        assertEquals("Bearer A2", retry?.header("Authorization"))
        assertEquals(listOf("R1"), api.sentTokens) // body { refresh_token } terkirim
        assertEquals("R2", tokens.refreshToken)
        assertFalse(expired)
    }

    @Test
    fun concurrent401_refreshOnlyOnce() {
        // Request kedua dikirim dengan A1 sebelum refresh selesai → cukup diulang dengan A2.
        val api = FakeRefreshApi(200)
        val auth = TokenAuthenticator(api, FakeTokenStore("A1", "R1")) {}
        auth.authenticate(null, unauthorized())
        val retry = auth.authenticate(null, unauthorized())
        assertEquals("Bearer A2", retry?.header("Authorization"))
        assertEquals(1, api.sentTokens.size) // R1 tidak dipakai 2x (BE akan me-revoke sesi)
    }

    @Test
    fun refreshRejected_expiresSession() {
        var expired = false
        val auth = TokenAuthenticator(FakeRefreshApi(401), FakeTokenStore("A1", "R1")) { expired = true }
        assertNull(auth.authenticate(null, unauthorized()))
        assertTrue(expired)
    }

    @Test
    fun noRefreshToken_expiresSession_withoutCallingServer() {
        // Sesi dari versi app lama (cookie): login lokal ada, token tidak.
        val api = FakeRefreshApi(200)
        var expired = false
        assertNull(TokenAuthenticator(api, FakeTokenStore()) { expired = true }.authenticate(null, unauthorized(bearer = null)))
        assertTrue(expired)
        assertTrue(api.sentTokens.isEmpty())
    }

    @Test
    fun networkError_doesNotExpireSession() {
        var expired = false
        val tokens = FakeTokenStore("A1", "R1")
        val auth = TokenAuthenticator(FakeRefreshApi(throwIo = true), tokens) { expired = true }
        assertNull(auth.authenticate(null, unauthorized()))
        assertFalse(expired)
        assertEquals("R1", tokens.refreshToken)
    }

    @Test
    fun serverError_doesNotExpireSession() {
        var expired = false
        val auth = TokenAuthenticator(FakeRefreshApi(500), FakeTokenStore("A1", "R1")) { expired = true }
        assertNull(auth.authenticate(null, unauthorized()))
        assertFalse(expired)
    }

    @Test
    fun loginAndRefresh401_areNotRefreshed() {
        val api = FakeRefreshApi(200)
        val auth = TokenAuthenticator(api, FakeTokenStore("A1", "R1")) {}
        assertNull(auth.authenticate(null, unauthorized(path = "auth/login")))
        assertNull(auth.authenticate(null, unauthorized(path = "auth/refresh")))
        assertTrue(api.sentTokens.isEmpty())
    }

    @Test
    fun logout401_isNotRefreshed() {
        // Refresh di sini bisa selesai setelah logout menghapus token → token baru tertinggal.
        val api = FakeRefreshApi(200)
        val tokens = FakeTokenStore("A1", "R1")
        assertNull(TokenAuthenticator(api, tokens) {}.authenticate(null, unauthorized(path = "auth/logout")))
        assertTrue(api.sentTokens.isEmpty())
        assertEquals("R1", tokens.refreshToken)
    }
}

class SafeApiCallTest {

    @Test
    fun nullData_isError() = runBlocking {
        val result = safeApiCall { RetrofitResponse.success(ApiResponse<String>(success = true)) }
        assertTrue(result is Result.Error)
    }

    @Test
    fun data_isSuccess() = runBlocking {
        val result = safeApiCall { RetrofitResponse.success(ApiResponse(success = true, data = "ok")) }
        assertEquals(Result.Success("ok"), result)
    }

    @Test
    fun unitCall_ignoresNullData() = runBlocking {
        val result = safeApiCallUnit { RetrofitResponse.success(ApiResponse<Unit>(success = true)) }
        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun validationError_zodIssueArray_parsesFieldErrors() = runBlocking {
        // Bentuk asli sales-kanvas-be (nestjs-zod): errors = array issue.
        val body = """
            {"success":false,"statusCode":400,"message":"Validation failed","errors":[
              {"code":"too_small","path":["username"],"message":"Username wajib diisi"},
              {"code":"too_small","path":["username"],"message":"pesan kedua"},
              {"code":"invalid_type","path":["salesKanvasPlanDetail",0,"qty"],"message":"harus angka"},
              {"code":"unrecognized_keys","path":[],"message":"Unrecognized key"}
            ]}
        """.trimIndent().toResponseBody("application/json".toMediaType())
        val result = safeApiCall<String> { RetrofitResponse.error(400, body) }
        result as Result.Error
        assertEquals("Validation failed", result.message)
        assertEquals(400, result.code)
        assertEquals("Username wajib diisi", result.fieldErrors?.get("username")) // pesan pertama
        assertEquals("harus angka", result.fieldErrors?.get("salesKanvasPlanDetail.0.qty"))
        assertEquals(2, result.fieldErrors?.size) // issue tanpa path diabaikan
    }

    @Test(expected = CancellationException::class)
    fun cancellation_isRethrown_notWrappedAsError(): Unit = runBlocking {
        safeApiCall<String> { throw CancellationException("layar ditutup") }
    }

    @Test
    fun unexpectedException_hidesRawMessage() = runBlocking {
        val result = safeApiCall<String> { throw IllegalStateException("Expected BEGIN_OBJECT but was STRING") }
        result as Result.Error
        assertEquals("Terjadi kesalahan tak terduga", result.message)
    }

    @Test
    fun validationError_objectMap_stillSupported() = runBlocking {
        val body = """{"success":false,"message":"Validasi gagal","errors":{"username":"wajib"}}"""
            .toResponseBody("application/json".toMediaType())
        val result = safeApiCall<String> { RetrofitResponse.error(400, body) }
        result as Result.Error
        assertEquals("wajib", result.fieldErrors?.get("username"))
    }
}
