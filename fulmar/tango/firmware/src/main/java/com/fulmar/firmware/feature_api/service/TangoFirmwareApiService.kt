package com.fulmar.firmware.feature_api.service

import com.fulmar.firmware.feature_api.config.TangoFirmwareApiConfig
import com.fulmar.firmware.feature_api.model.CheckAndFetchInput
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TangoFirmwareApiService {
    @POST(TangoFirmwareApiConfig.URL_CHECK_AND_FETCH)
    @Headers("Content-Type: application/json")
    suspend fun checkAndFetch(@Body input: CheckAndFetchInput): Response<ResponseBody>
}