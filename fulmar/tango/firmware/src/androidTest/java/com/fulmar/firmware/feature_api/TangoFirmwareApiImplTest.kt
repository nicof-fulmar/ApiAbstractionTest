package com.fulmar.firmware.feature_api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fulmar.api.model.ApiCertificateInput
import com.supermegazinc.escentials.Result
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TangoFirmwareApiImplTest {

    private lateinit var tangoFirmwareApi: TangoFirmwareApiImpl

    @Before
    fun setup() {
        tangoFirmwareApi = TangoFirmwareApiImpl(
            urlBase = "https://api3.ful-mar.net",
            certificate = ApiCertificateInput(
                domain = "api3.ful-mar.net",
                certificatePinSHA256 = "sha256/jU8P1uAp6g/xcQg/DeGxsi33poQjhMT9wmRFzR/AKsI="
            )
        )
    }

    @Test
    fun getLatestFirmwareVersion() = runBlocking {
        val result = tangoFirmwareApi.getLatestFirmwareVersion()
        (result as? Result.Success)?.data?.data?.actualFirmware?.let {
            println("Ultima version de firmware: $it")
        }
        assert(result is Result.Success)
    }

    @Test
    fun fetchFile() = runBlocking {
        val result = tangoFirmwareApi.fetchFile()
        (result as? Result.Success)?.data?.size?.let {
            println("Descargado: $it bytes")
        }
        assert(result is Result.Success)
    }
}