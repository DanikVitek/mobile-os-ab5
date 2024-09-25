package me.danikvitek.lab5.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import me.danikvitek.lab5.service.ChemEngineService
import okhttp3.MediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RetrofitModule {
    @get:Provides
    @get:Singleton
    val chemEngineService: ChemEngineService by lazy {
        Retrofit.Builder()
            .baseUrl("http://chemengine.kpi.ua/")
            .addConverterFactory(Json.asConverterFactory(MediaType.get("application/json")))
            .build()
            .create()
    }
}