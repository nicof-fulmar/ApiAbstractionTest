package com.fulmar.firmware.feature_api

import com.fulmar.api.model.ApiGenericResponse
import com.fulmar.api.model.ApiResponseError
import com.fulmar.firmware.feature_api.model.GetLatestFirmwareVersionOutput
import com.supermegazinc.escentials.Result

interface TangoFirmwareApi {
    suspend fun getLatestFirmwareVersion(): Result<GetLatestFirmwareVersionOutput, ApiResponseError>
    suspend fun fetchFile(): Result<ByteArray, ApiResponseError>
}