package com.fulmar.firmware.feature_api

import com.fulmar.api.model.ApiResponseError
import com.fulmar.firmware.feature_api.model.CheckAndFetchInput
import com.fulmar.firmware.feature_api.model.CheckAndFetchOutput
import com.supermegazinc.escentials.Result

interface TangoFirmwareApi {
    suspend fun checkAndFetch(input: CheckAndFetchInput): Result<CheckAndFetchOutput, ApiResponseError>
}