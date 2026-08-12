package com.kidzone.data.repository

import com.kidzone.data.remote.IpGeolocationApi
import com.kidzone.domain.repository.IpLocationRepository
import com.kidzone.utils.OpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [IpLocationRepository] oparta na Retrofit i serwisie ip-api.com.
 */
@Singleton
class RetrofitIpLocationRepository @Inject constructor(
    private val api: IpGeolocationApi
) : IpLocationRepository {

    override suspend fun getApproximateLocation(): OpResult<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getLocation()
            if (response.status == "success" && response.lat != null && response.lon != null) {
                OpResult.success(response.lat to response.lon)
            } else {
                OpResult.failure(Exception(response.message ?: "Unknown API error"))
            }
        }.getOrElse {
            OpResult.failure(it)
        }
    }
}
