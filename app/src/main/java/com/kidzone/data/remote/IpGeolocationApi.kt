package com.kidzone.data.remote

import com.kidzone.data.remote.dto.IpGeolocationDto
import retrofit2.http.GET

/**
 * Interfejs Retrofit dla serwisu geolokalizacji po IP.
 */
interface IpGeolocationApi {

    companion object {
        const val BASE_URL = "http://ip-api.com/"
    }

    @GET("json/")
    suspend fun getLocation(): IpGeolocationDto
}
