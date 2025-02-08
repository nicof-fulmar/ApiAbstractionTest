package com.fulmar.firmware.feature_api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fulmar.api.createApiService
import com.fulmar.api.model.ApiCertificateInput
import com.fulmar.firmware.TestLogger
import com.fulmar.firmware.feature_api.model.CheckAndFetchInput
import com.fulmar.firmware.feature_api.model.CheckAndFetchOutput
import com.fulmar.firmware.feature_api.service.TangoFirmwareApiService
import com.supermegazinc.escentials.Result
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirmwareApiImplTest {

    private val logger = TestLogger()

    private lateinit var tangoFirmwareApi: TangoFirmwareApi

    @Before
    fun setup() {
        tangoFirmwareApi = TangoFirmwareApiImpl(
            logger = logger,
            serviceFactory = {
                createApiService(
                    urlBase = "https://api3.ful-mar.net",
                    certificate = ApiCertificateInput(
                        domain = "api3.ful-mar.net",
                        certificatePinSHA256 = "sha256/jU8P1uAp6g/xcQg/DeGxsi33poQjhMT9wmRFzR/AKsI="
                    ),
                    logger = logger,
                    serviceClass = TangoFirmwareApiService::class.java
                )
            }
        )
    }

    @Test
    fun newVersionAvailable() = runBlocking {
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.4"))
        val testResult = ((result as? Result.Success)?.data as? CheckAndFetchOutput.Firmware)?.also { firmware->
            logger.i("TEST", "Version: ${firmware.version}, Tamaño: ${firmware.firmware.size}")
        }
        assert(testResult!=null)
    }

    @Test
    fun versionAlreadyUpdated() = runBlocking {
        val result = tangoFirmwareApi.checkAndFetch(CheckAndFetchInput("A1.0.5"))
        val testResult = ((result as? Result.Success)?.data as? CheckAndFetchOutput.AlreadyUpdated)?.also {
            logger.i("TEST", "Ya esta instalada la ultima version")
        }
        assert(testResult!=null)
    }

}