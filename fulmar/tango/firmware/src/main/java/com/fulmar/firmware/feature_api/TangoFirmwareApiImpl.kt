package com.fulmar.firmware.feature_api

import com.fulmar.api.createApiService
import com.fulmar.api.model.ApiCertificateInput
import com.fulmar.api.model.ApiGenericResponse
import com.fulmar.api.model.ApiResponseError
import com.fulmar.api.utils.makeApiCall
import com.fulmar.firmware.feature_api.model.GetLatestFirmwareVersionOutput
import com.fulmar.firmware.feature_api.service.TangoFirmwareApiService
import com.supermegazinc.escentials.Result

class TangoFirmwareApiImpl(
    urlBase: String,
    certificate: ApiCertificateInput
) : TangoFirmwareApi {

    private val service = createApiService(TangoFirmwareApiService::class.java, urlBase, certificate)
    
    override suspend fun getLatestFirmwareVersion(): Result<ApiGenericResponse<GetLatestFirmwareVersionOutput>, ApiResponseError> {
        return makeApiCall {
            service.getLatestFirmwareVersion()
        }
    }

    override suspend fun fetchFile(): Result<ByteArray, ApiResponseError> {
        val response = makeApiCall {
            service.fetchFile()
        }
        return when(response) {
            is Result.Fail -> {
               Result.Fail(response.error)
            }
            is Result.Success -> {
                Result.Success(response.data.bytes())
            }
        }
    }
}