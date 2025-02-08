package com.fulmar.firmware.feature_api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fulmar.api.model.ApiGenericResponse
import com.fulmar.api.model.ApiResponseError
import com.fulmar.firmware.TestLogger
import com.fulmar.firmware.feature_api.model.CheckAndFetchInput
import com.fulmar.firmware.feature_api.model.CheckAndFetchOutput
import com.fulmar.firmware.feature_api.utils.FirmwareApiServiceTestCase
import com.fulmar.firmware.feature_api.utils.createMockFirmwareApiService
import com.supermegazinc.escentials.Result
import com.supermegazinc.json.strictGson
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirmwareApiImplMockTest {

    private val logger = TestLogger()

    @Test
    fun newVersionAvailable() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.NEW_VERSION_AVAILABLE)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Success)?.data as? CheckAndFetchOutput.Firmware)?.also { firmware->
            logger.i("TEST", "Version: ${firmware.version}, Firmware: ${firmware.firmware.decodeToString()}")
        }
        assert(testResult!=null)
    }

    @Test
    fun versionAlreadyUpdated() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.VERSION_ALREADY_UPDATED)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Success)?.data as? CheckAndFetchOutput.AlreadyUpdated)?.also {
            logger.i("TEST", "Ya esta instalada la ultima version")
        }
        assert(testResult!=null)
    }

    @Test
    fun networkError() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.NETWORK_ERROR)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Fail)?.error as? ApiResponseError.BadRequest)?.also {
            logger.i("TEST", result.error.toString())
        }
        assert(testResult!=null)
    }

    @Test
    fun noContentType() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.NO_CONTENT_TYPE)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Fail)?.error as? ApiResponseError.BadResponse)?.also {
            logger.i("TEST", result.error.toString())
        }
        assert(testResult!=null)
    }

    @Test
    fun noBody() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.NO_BODY)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Fail)?.error as? ApiResponseError.BadResponse)?.also {
            logger.i("TEST", result.error.toString())
        }
        assert(testResult!=null)
    }

    @Test
    fun noVersion() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.NO_VERSION)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Fail)?.error as? ApiResponseError.BadResponse)?.also {
            logger.i("TEST", result.error.toString())
        }
        assert(testResult!=null)
    }

    @Test
    fun wrongCode() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.WRONG_CODE)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Fail)?.error as? ApiResponseError.BadResponse)?.also {
            logger.i("TEST", result.error.toString())
        }
        assert(testResult!=null)
    }

    @Test
    fun wrongJson() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.WRONG_JSON)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Fail)?.error as? ApiResponseError.BadResponse)?.also {
            logger.i("TEST", result.error.toString())
        }
        assert(testResult!=null)
    }

    @Test
    fun wrongContentType() = runBlocking {
        val tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createMockFirmwareApiService(FirmwareApiServiceTestCase.WRONG_CONTENT_TYPE)
            }
        )
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.0"))
        val testResult = ((result as? Result.Fail)?.error as? ApiResponseError.BadResponse)?.also {
            logger.i("TEST", result.error.toString())
        }
        assert(testResult!=null)
    }

    @Test
    fun jsonIntegrityCheck() = runBlocking {
        val checkAndFetchInputJson = "{\n\"CurrentFirmwareVersion\": \"A1.0.5\"\n}"
        val checkAndFetchOutputJson = "{\n" +
                "    \"statusCode\": 1,\n" +
                "    \"data\": null,\n" +
                "    \"message\": \"El firmware está actualizado.\"\n" +
                "}"

        val testResult = try {
            strictGson().fromJson(checkAndFetchInputJson, CheckAndFetchInput::class.java)!!
            strictGson().fromJson(checkAndFetchOutputJson, ApiGenericResponse::class.java)!!
        } catch (e: Exception) {
            println(e)
            null
        }
        assert(testResult!=null)
    }
}