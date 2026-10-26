package com.mhyddnn.mubarokappmobile.core.network.di

import android.content.Context
import com.mhyddnn.mubarokappmobile.BuildConfig
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.network.AUTHORIZATION
import com.mhyddnn.mubarokappmobile.core.network.AuthInterceptor
import com.mhyddnn.mubarokappmobile.core.network.PrefsTokenStore
import com.mhyddnn.mubarokappmobile.core.network.ServerUrlInterceptor
import com.mhyddnn.mubarokappmobile.core.network.TokenAuthenticator
import com.mhyddnn.mubarokappmobile.core.network.TokenRefreshApi
import com.mhyddnn.mubarokappmobile.core.network.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Penanda OkHttp/Retrofit khusus untuk endpoint refresh (tanpa authenticator). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient

/**
 * Dua client OkHttp:
 * - **utama** (semua API fitur + outbox): [ServerUrlInterceptor] → [AuthInterceptor] (Bearer) →
 *   log, dengan [TokenAuthenticator] untuk 401 → refresh → ulang request.
 * - **[RefreshClient]**: hanya `POST /auth/refresh`, tanpa Bearer & tanpa authenticator, agar 401
 *   dari refresh tidak memicu refresh lagi (rekursi).
 *
 * Urutan interceptor penting: URL diganti dulu, lalu Bearer, log paling akhir supaya yang
 * tercatat adalah request final (dengan header `Authorization` disensor).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Singleton: satu sumber token untuk interceptor, authenticator, dan logout. */
    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore =
        PrefsTokenStore(context)

    /** Log client utama: BODY di debug (payload API terlihat), mati di release. */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        logging(HttpLoggingInterceptor.Level.BODY)

    /** Logcat bisa dibaca lewat adb / ikut tertempel di laporan bug → Bearer selalu disensor. */
    private fun logging(debugLevel: HttpLoggingInterceptor.Level) = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) debugLevel else HttpLoggingInterceptor.Level.NONE
        redactHeader(AUTHORIZATION)
    }

    // --- Client khusus refresh (tanpa authenticator & Bearer → mencegah rekursi 401) ---

    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshClient(
        serverUrl: ServerUrlInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(serverUrl)
            // HEADERS, bukan BODY: body request & respons refresh berisi refresh_token.
            .addInterceptor(logging(HttpLoggingInterceptor.Level.HEADERS))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        @RefreshClient refreshClient: OkHttpClient,
        tokens: TokenStore,
        sessionManager: SessionManager,
    ): TokenAuthenticator {
        val refreshApi = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(refreshClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TokenRefreshApi::class.java)
        // Sesi lokal TIDAK dihapus (app tetap jalan offline, outbox aman) — cukup tandai
        // needsRelogin; UI menampilkan ajakan login ulang dan sinkron berhenti sampai login.
        // runBlocking: callback ini dipanggil di thread OkHttp (dari authenticate), bukan main.
        return TokenAuthenticator(refreshApi, tokens, onSessionExpired = {
            tokens.clear()
            runBlocking { sessionManager.markNeedsRelogin() }
        })
    }

    // --- Client utama (Bearer + auto-refresh) ---

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokens: TokenStore,
        logging: HttpLoggingInterceptor,
        authenticator: TokenAuthenticator,
        serverUrl: ServerUrlInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(serverUrl)
            .addInterceptor(AuthInterceptor(tokens))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
