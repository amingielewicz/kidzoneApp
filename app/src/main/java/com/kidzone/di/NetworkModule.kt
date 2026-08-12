package com.kidzone.di

import com.kidzone.data.remote.IpGeolocationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Dostarczanie instancji Retrofit oraz interfejsów API do komunikacji sieciowej.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(IpGeolocationApi.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideIpGeolocationApi(retrofit: Retrofit): IpGeolocationApi =
        retrofit.create(IpGeolocationApi::class.java)
}
