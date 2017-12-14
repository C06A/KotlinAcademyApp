package org.kotlinacademy.backend.repositories.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kotlinacademy.backend.Config
import org.kotlinacademy.gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

fun makeRetrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(makeHttpClient())
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()!!

private fun makeHttpClient() = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor())
        .build()

private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
    level = if (Config.production) NONE else BODY
}