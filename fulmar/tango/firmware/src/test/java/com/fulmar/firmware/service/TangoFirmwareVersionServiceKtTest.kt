package com.fulmar.firmware.service

import com.supermegazinc.escentials.waitForNextWithTimeout
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TangoFirmwareVersionServiceKtTest {

    private class TestLogger : Logger {
        override fun d(tag: String?, message: String) = println("D[$tag] - $message")
        override fun e(tag: String?, message: String) = println("E[$tag] - $message")
        override fun i(tag: String?, message: String) = println("I[$tag] - $message")
    }

    private fun runFirmwareCheckTest(
        installedVersion: String?,
        apiVersion: String?,
        expectedResult: Boolean?
    ) = runBlocking {
        val connectedFlow = MutableStateFlow(true)
        val resultFlow = MutableSharedFlow<Boolean>()

        val job = launch {
            tangoFirmwareVersionService(
                connectedFlow,
                onRequestTangoCurrentFirmwareVersion = { installedVersion },
                onRequestApiLatestFirmwareVersion = { apiVersion },
                onFirmwareUpdate = { launch { resultFlow.emit(true) } },
                LOG_KEY = "TANGO-FMW",
                logger = TestLogger()
            )
        }

        val result = try {
            resultFlow.waitForNextWithTimeout(500)
        } catch (e: TimeoutCancellationException) {
            null
        }

        assertEquals(expectedResult, result)

        job.cancelAndJoin()
    }

    @Test
    fun `Ultima version mayor a la instalada`() = runBlocking {
        runFirmwareCheckTest("1.0.0", "1.0.1", true)
    }

    @Test
    fun `Ultima version igual a la instalada`() = runBlocking {
        runFirmwareCheckTest("1.0.0", "1.0.0", null)
    }

    @Test
    fun `Ultima version menor a la instalada`() = runBlocking {
        runFirmwareCheckTest("1.0.1", "1.0.0", null)
    }

    @Test
    fun `Error al obtener ultima version`() = runBlocking {
        runFirmwareCheckTest("1.0.0", null, null)
    }

    @Test
    fun `Error al obtener version instalada`() = runBlocking {
        runFirmwareCheckTest(null, "1.0.0", null)
    }

    @Test
    fun `Chequeo cancelado`() = runBlocking {
        val connectedFlow = MutableStateFlow(true)
        val resultFlow = MutableSharedFlow<Boolean>()

        launch {
            delay(500)
            connectedFlow.update { false }
        }

        val job = launch {
            tangoFirmwareVersionService(
                connectedFlow,
                onRequestTangoCurrentFirmwareVersion = { delay(1000); "1.0.0" },
                onRequestApiLatestFirmwareVersion = { delay(1000); "1.0.1" },
                onFirmwareUpdate = { launch { resultFlow.emit(true) } },
                LOG_KEY = "TANGO-FMW",
                logger = TestLogger()
            )
        }

        val result = try {
            resultFlow.waitForNextWithTimeout(2000)
        } catch (e: TimeoutCancellationException) {
            null
        }

        assertEquals(null, result)

        job.cancelAndJoin()
    }
}
