package me.danikvitek.lab5.service

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface ChemEngineService {
    // Downloads a PDF file
    @Headers("Accept: application/pdf")
    @GET("article/download/228078/227310/519284")
    suspend fun downloadPdf(): Response<ResponseBody>
}