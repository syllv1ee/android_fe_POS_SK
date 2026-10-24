package com.mhyddnn.mubarokappmobile.core.network.di

import android.content.Context
import com.mhyddnn.mubarokappmobile.BuildConfig
import com.mhyddnn.mubarokappmobile.core.datastore.SessionManager
import com.mhyddnn.mubarokappmobile.core.network.ServerUrlInterceptor
import com.mhyddnn.mubarokappmobile.core.network.SessionCookieJar
import com.mhyddnn.mubarokappmobile.core.network.TokenAuthenticator
import com.mhyddnn.mubarokappmobile.core.network.TokenRefreshApi
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

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCookieJar(@ApplicationContext context: Context): SessionCookieJar =
        SessionCookieJar(context)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    // --- Client khusus refresh (tanpa authenticator → mencegah rekursi 401) ---

    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshClient(
        cookieJar: SessionCookieJar,
        logging: HttpLoggingInterceptor,
        serverUrl: ServerUrlInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(serverUrl)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        @RefreshClient refreshClient: OkHttpClient,
        cookieJar: SessionCookieJar,
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
        return TokenAuthenticator(refreshApi, onSessionExpired = {
            cookieJar.clear()
            runBlocking { sessionManager.markNeedsRelogin() }
        })
    }

    // --- Client utama (dengan cookie + auto-refresh) ---

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: SessionCookieJar,
        logging: HttpLoggingInterceptor,
        authenticator: TokenAuthenticator,
        serverUrl: ServerUrlInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .authenticator(authenticator)
            .addInterceptor(serverUrl)
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
