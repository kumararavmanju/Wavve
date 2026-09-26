package com.aravind.wavve.data.piped

import retrofit2.http.GET

interface PipedInstancesApi {
    @GET("/")
    suspend fun getInstances(): List<PipedInstance>
}