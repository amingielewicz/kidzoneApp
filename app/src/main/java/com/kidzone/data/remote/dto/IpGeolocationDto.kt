package com.kidzone.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO dla odpowiedzi z API geolokalizacji IP (np. ip-api.com).
 */
data class IpGeolocationDto(
    @SerializedName("status") val status: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
    @SerializedName("city") val city: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("message") val message: String?
)
