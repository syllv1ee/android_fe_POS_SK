package com.mhyddnn.mubarokappmobile.core.network

import com.mhyddnn.mubarokappmobile.core.common.Result
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import okhttp3.Response as OkResponse
import retrofit2.Response as RetrofitResponse

class TokenAuthenticatorTest {

    private class FakeRefreshApi(var code: Int = 200, var throwIo: Boolean = false) : TokenRefreshApi {
        var calls = 0
        override suspend fun refresh(): RetrofitResponse<Unit> {
            calls++
            if (throwIo) throw IOException("offline")
            return if (code in 200..299) {
                RetrofitResponse.success(code, Unit)
            } else {
                RetrofitResponse.error(code, "".toResponseBody())
            }
        }
    }

    private fun unauthorized(path: String = "sales", sentAt: Long = System.currentTimeMillis()) =
        OkResponse.Builder()
            .request(Request.Builder().url("http://x/api/$path").build())
            .protocol(Protocol.HTTP_1_1)
            .code(401).message("Unauthorized")
            .sentRequestAtMillis(sentAt)
            .build()

    @Test
    fun refreshSuccess_retriesRequest() {
        val api = FakeRefreshApi(200)
        var expired = false
        assertNotNull(TokenAuthenticator(api) { expired = true }.authenticate(null, unauthorized()))
        assertEquals(1, api.calls)
        assertTrue(!expired)
    }

    @Test
    fun refreshRejected_expiresSession() {
        var expired = false
        assertNull(TokenAuthenticator(FakeRefreshApi(401)) { expired = true }.authenticate(null, unauthorized()))
        assertTrue(expired)
    }

    @Test
    fun networkError_doesNotExpireSession() {
        var expired = false
        val auth = TokenAuthenticator(FakeRefreshApi(throwIo = true)) { expired = true }
        assertNull(auth.authenticate(null, unauthorized()))
        assertTrue(!expired)
    }

    @Test
    fun serverError_doesNotExpireSession() {
        var expired = false
        assertNull(TokenAuthenticator(FakeRefreshApi(500)) { expired = true }.authenticate(null, unauthorized()))
        assertTrue(!expired)
    }

    @Test
    fun requestSentBeforeLastRefresh_retriesWithoutRefreshingAgain() {
        val api = FakeRefreshApi(200)
        val auth = TokenAuthenticator(api) {}
        auth.authenticate(null, unauthorized())
        assertNotNull(auth.authenticate(null, unauthorized(sentAt = 0)))
        assertEquals(1, api.calls)
    }

    @Test
    fun loginUnauthorized_isNotRefreshed() {
        val api = FakeRefreshApi(200)
        assertNull(TokenAuthenticator(api) {}.authenticate(null, unauthorized(path = "auth/login")))
        assertEquals(0, api.calls)
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
    fun validationError_parsesFieldErrors() = runBlocking {
        val body = """{"success":false,"message":"Validasi gagal","errors":{"username":"wajib"}}"""
            .toResponseBody("application/json".toMediaType())
        val result = safeApiCall<String> { RetrofitResponse.error(400, body) }
        result as Result.Error
        assertEquals("Validasi gagal", result.message)
        assertEquals(400, result.code)
        assertEquals("wajib", result.fieldErrors?.get("username"))
    }
}
