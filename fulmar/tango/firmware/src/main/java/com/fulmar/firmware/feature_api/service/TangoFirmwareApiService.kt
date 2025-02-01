package com.fulmar.firmware.feature_api.service

import com.fulmar.api.model.ApiGenericResponse
import com.fulmar.firmware.feature_api.config.TangoFirmwareApiConfig
import com.fulmar.firmware.feature_api.model.GetLatestFirmwareVersionOutput
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers


interface TangoFirmwareApiService {
    @GET(TangoFirmwareApiConfig.URL_GET_LATEST_FIRMWARE_VERSION)
    @Headers("Content-Type: application/json")
    suspend fun getLatestFirmwareVersion(): Response<ApiGenericResponse<GetLatestFirmwareVersionOutput>>

    @GET(TangoFirmwareApiConfig.URL_FETCH_FILE)
    suspend fun fetchFile(): Response<ResponseBody>
}