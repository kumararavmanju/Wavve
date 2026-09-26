package com.aravind.wavve.di

import android.content.Context
import androidx.room.Room
import com.aravind.wavve.data.db.AppDatabase
import com.aravind.wavve.data.itunes.ItunesApi
import com.aravind.wavve.data.piped.PipedApi
import com.aravind.wavve.data.piped.PipedInstanceProvider
import com.aravind.wavve.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private class PipedFailoverInterceptor(
        private val instanceProvider: PipedInstanceProvider
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            var lastResponse: Response? = null
            var lastException: IOException? = null

            val instances = instanceProvider.getInstances()

            for (instance in instances) {
                val instanceUrl = instance.toHttpUrl()
                val newUrl = originalRequest.url.newBuilder()
                    .scheme(instanceUrl.scheme)
                    .host(instanceUrl.host)
                    .build()

                val newRequest = originalRequest.newBuilder()
                    .url(newUrl)
                    .build()

                try {
                    val response = chain.proceed(newRequest)

                    if (response.isSuccessful) {
                        val peeked = runCatching { response.peekBody(128).string() }.getOrNull().orEmpty()
                        val looksDead = peeked.contains("shutdown", ignoreCase = true) ||
                                peeked.contains("has been discontinued", ignoreCase = true) ||
                                peeked.contains("instance is down", ignoreCase = true)

                        if (looksDead) {
                            response.close()
                            continue
                        }
                        return response
                    }

                    if (response.code == 403 || response.code == 429 || response.code >= 500) {
                        lastResponse = response
                        response.close()
                        continue
                    }

                    return response
                } catch (e: IOException) {
                    lastException = e
                }
            }

            lastResponse?.let { return it }
            throw lastException ?: IOException("All Piped instances failed")
        }
    }

    @Provides
    @Singleton
    fun providePipedInstanceProvider(): PipedInstanceProvider = PipedInstanceProvider()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @PipedHttpClient
    fun providePipedOkHttpClient(instanceProvider: PipedInstanceProvider): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(PipedFailoverInterceptor(instanceProvider))
            .addInterceptor(logging)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun providePipedApi(@PipedHttpClient okHttpClient: OkHttpClient): PipedApi =
        Retrofit.Builder()
            .baseUrl(Constants.PIPED_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)

    /** Step 8: iTunes Search API — free, unauthenticated, no key needed. Used
     *  only to disambiguate a raw search query into a clean title+artist
     *  before checking local device storage / YouTube. Uses the plain default
     *  OkHttpClient — no failover needed, this is a single stable Apple endpoint. */
    @Provides
    @Singleton
    fun provideItunesApi(okHttpClient: OkHttpClient): ItunesApi =
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ItunesApi::class.java)

    /** fallbackToDestructiveMigration(): schema now at v3 (Step 8 added
     *  localContentUri to songs) with exportSchema = false and no Migration
     *  objects — without this, Room throws on version mismatch instead of
     *  recreating the DB. Acceptable during active development. */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "wavve.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSongDao(db: AppDatabase) = db.songDao()
    @Provides fun providePlaylistDao(db: AppDatabase) = db.playlistDao()
    @Provides fun provideAlbumDao(db: AppDatabase) = db.albumDao()
    @Provides fun provideEqPresetDao(db: AppDatabase) = db.eqPresetDao()
    @Provides fun providePodcastStateDao(db: AppDatabase) = db.podcastStateDao()
    @Provides fun provideRecentSearchDao(db: AppDatabase) = db.recentSearchDao()
    @Provides fun provideSearchQuotaDao(db: AppDatabase) = db.searchQuotaDao()
}