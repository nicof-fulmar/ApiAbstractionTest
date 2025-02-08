package com.fulmar.firmware.feature_api

import com.fulmar.api.model.ApiGenericResponse
import com.fulmar.api.model.ApiResponseError
import com.fulmar.api.utils.makeApiCallRaw
import com.fulmar.firmware.feature_api.model.CheckAndFetchInput
import com.fulmar.firmware.feature_api.model.CheckAndFetchOutput
import com.fulmar.firmware.feature_api.service.TangoFirmwareApiService
import com.supermegazinc.escentials.Result
import com.supermegazinc.json.strictGson
import com.supermegazinc.logger.Logger

class TangoFirmwareApiImpl(
    private val logger: Logger,
    serviceFactory: () -> TangoFirmwareApiService
) : TangoFirmwareApi {

    private companion object {
        const val LOG_KEY = "TANGO-FMW-API"
    }

    private val service = serviceFactory()

    override suspend fun checkAndFetch(input: CheckAndFetchInput): Result<CheckAndFetchOutput, ApiResponseError> {
        val result = makeApiCallRaw {
            service.checkAndFetch(input)
        }
        when(result) {
            is Result.Fail -> {
                return Result.Fail(result.error)
            }
            is Result.Success -> {
                val response = result.data
                val contentType = response.headers()["Content-Type"] ?: run {
                    logger.e(LOG_KEY, "No se encontro el Content-Type")
                    return Result.Fail(ApiResponseError.BadResponse)
                }

                val bodyBytes = try {
                    response.body()?.bytes()!!
                } catch (e: Exception) {
                    logger.e(LOG_KEY, "No se encontro el body: ${e.message}")
                    return Result.Fail(ApiResponseError.BadResponse)
                }

                return when {
                    contentType.contains("application/octet-stream") -> {
                        val version = response.headers()["Firmware-Version"]?.takeIf { it.isNotBlank() } ?: run {
                            logger.e(LOG_KEY, "No se encontro la version")
                            return Result.Fail(ApiResponseError.BadResponse)
                        }
                        Result.Success(CheckAndFetchOutput.Firmware(version, bodyBytes))
                    }
                    contentType.contains("application/json") -> {
                        val deserialized = try {
                            strictGson().fromJson(bodyBytes.decodeToString(), ApiGenericResponse::class.java)!!
                        } catch (e: Exception) {
                            println(e)
                            logger.e(LOG_KEY, "No se pudo deserializar")
                            return Result.Fail(ApiResponseError.BadResponse)
                        }

                        if(deserialized.statusCode==1) Result.Success(CheckAndFetchOutput.AlreadyUpdated)
                        else {
                            logger.e(LOG_KEY, "No se pudo identificar el codigo: ${deserialized.statusCode}")
                            Result.Fail(ApiResponseError.BadResponse)
                        }
                    }
                    else -> {
                        logger.e(LOG_KEY, "No se pudo identificar el Content-Type: '$contentType'")
                        Result.Fail(ApiResponseError.BadResponse)
                    }
                }
            }
        }
    }
}