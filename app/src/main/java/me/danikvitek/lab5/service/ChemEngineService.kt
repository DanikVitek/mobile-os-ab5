package me.danikvitek.lab5.service

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Streaming

interface ChemEngineService {
    @Headers("Accept: application/pdf")
    @GET("article/download/228078/227310/519284")
//    @GET("article/download/228078/227310/519285")
    @Streaming
    suspend fun downloadPdf(): Response<ResponseBody>
}