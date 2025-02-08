package com.fulmar.api

import com.fulmar.api.di.ApiModuleInitializer
import com.fulmar.api.model.ApiCertificateInput
import com.supermegazinc.logger.Logger
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

fun <T: Any>createApiService(
    serviceClass: Class<T>,
    urlBase: String,
    certificate: ApiCertificateInput?,
    logger: Logger = ApiModuleInitializer._logger
): T {

    logger.d("API-SERV", "Creando servicio: ${serviceClass::class.java.simpleName}")

    val okHttpClientBuilder = OkHttpClient.Builder()

    okHttpClientBuilder.connectTimeout(30, TimeUnit.SECONDS)

    certificate?.let {
        val certificatePinner = CertificatePinner.Builder()
            .add(certificate.domain, certificate.certificatePinSHA256)
            .build()
        okHttpClientBuilder.certificatePinner(certificatePinner)
    }

    val okHttpClient = okHttpClientBuilder.build()

    return Retrofit.Builder()
        .baseUrl(urlBase)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(serviceClass)
}