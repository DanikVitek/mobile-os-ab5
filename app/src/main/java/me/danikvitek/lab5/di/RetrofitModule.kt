package me.danikvitek.lab5.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.danikvitek.lab5.service.ChemEngineService
import retrofit2.Retrofit
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
            .build()
            .create()
    }
}