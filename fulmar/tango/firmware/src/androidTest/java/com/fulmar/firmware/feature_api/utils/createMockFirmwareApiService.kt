package com.fulmar.firmware.feature_api.utils

import com.fulmar.firmware.feature_api.model.CheckAndFetchInput
import com.fulmar.firmware.feature_api.service.TangoFirmwareApiService
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body

fun createMockFirmwareApiService(testCase: FirmwareApiServiceTestCase): TangoFirmwareApiService {
    return object : TangoFirmwareApiService {
        override suspend fun checkAndFetch(@Body input: CheckAndFetchInput): Response<ResponseBody> {
            return when (testCase) {
                FirmwareApiServiceTestCase.NEW_VERSION_AVAILABLE -> {
                    Response.success(
                        ResponseBody
                            .create(
                                null,
                                "firmware"
                            ),
                        MockOkHttp3ResponseBuilder()
                            .addHeader("Content-Type", "application/octet-stream")
                            .addHeader("Firmware-Version", "1.0.0")
                            .code(200)
                            .build()
                    )
                }

                FirmwareApiServiceTestCase.VERSION_ALREADY_UPDATED -> {
                    Response.success(
                        ResponseBody
                            .create(
                                null,
                                "{\n" +
                                        "    \"statusCode\": 1,\n" +
                                        "    \"data\": null,\n" +
                                        "    \"message\": \"El firmware está actualizado.\"\n" +
                                        "}"
                            ),
                        MockOkHttp3ResponseBuilder()
                            .addHeader("Content-Type", "application/json")
                            .code(200)
                            .build()
                    )
                }

                FirmwareApiServiceTestCase.NETWORK_ERROR -> {
                    Response.error(
                        ResponseBody
                            .create(
                                null,
                                ""
                            ),
                        MockOkHttp3ResponseBuilder()
                            .addHeader("Content-Type", "application/json")
                            .code(400)
                            .build()
                    )
                }

                FirmwareApiServiceTestCase.NO_CONTENT_TYPE -> {
                    Response.success(
                        ResponseBody
                            .create(
                                null,
                                "{\n" +
                                        "    \"statusCode\": 1,\n" +
                                        "    \"data\": null,\n" +
                                        "    \"message\": \"El firmware está actualizado.\"\n" +
                                        "}"
                            ),
                        MockOkHttp3ResponseBuilder()
                            .code(200)
                            .build()
                    )
                }

                FirmwareApiServiceTestCase.NO_BODY -> {
                    Response.success(
                        null,
                        MockOkHttp3ResponseBuilder()
                            .addHeader("Content-Type", "application/json")
                            .code(200)
                            .build()
                    )
                }

                FirmwareApiServiceTestCase.NO_VERSION -> {
                    Response.success(
                        ResponseBody
                            .create(
                                null,
                                "firmware"
                            ),
                        MockOkHttp3ResponseBuilder()
                            .addHeader("Content-Type", "application/octet-stream")
                            .code(200)
                            .build()
                    )
                }

                FirmwareApiServiceTestCase.WRONG_CODE -> {
                    Response.success(
                        ResponseBody
                            .create(
                                null,
                                "{\n" +
                                        "    \"statusCode\": 2,\n" +
                                        "    \"data\": null,\n" +
                                        "    \"message\": \"El firmware está actualizado.\"\n" +
                                        "}"
                            ),
                        MockOkHttp3ResponseBuilder()
                            .addHeader("Content-Type", "application/json")
                            .code(200)
                            .build()
                    )
                }

                FirmwareApiServiceTestCase.WRONG_JSON -> {
                    Response.success(
                        ResponseBody
                            .create(
                                null,
                                "{\n" +
                                        "    \"sdffds\": 1,\n" +
                                        "    \"fdsdsfdsf\": null,\n" +
                                        "    \"sfdfds\": \"El firmware está actualizado.\"\n" +
                                        "}"
                            ),
                        MockOkHttp3ResponseBuilder()
                            .addHeader("Content-Type", "application/json")
                            .code(200)
                            .build()
                    )
                }

                FirmwareApiServiceTestCase.WRONG_CONTENT_TYPE -> {
                    Response.success(
                        ResponseBody
                            .create(
                                null,
                                "{\n" +
                                        "    \"statusCode\": 1,\n" +
                                        "    \"data\": null,\n" +
                                        "    \"message\": \"El firmware está actualizado.\"\n" +
                                        "}"
                            ),
                        MockOkHttp3ResponseBuilder()
                            .addHeader("Content-Type", "application/wrongContentType")
                            .code(200)
                            .build()
                    )
                }
            }
        }
    }
}

enum class FirmwareApiServiceTestCase {
    NEW_VERSION_AVAILABLE,
    VERSION_ALREADY_UPDATED,
    NETWORK_ERROR,
    NO_CONTENT_TYPE,
    NO_BODY,
    NO_VERSION,
    WRONG_CODE,
    WRONG_JSON,
    WRONG_CONTENT_TYPE
}