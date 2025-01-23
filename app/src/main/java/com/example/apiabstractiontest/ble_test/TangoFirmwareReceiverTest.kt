package com.example.apiabstractiontest.ble_test

import com.fulmar.firmware.model.TangoFirmwareInitJson
import com.fulmar.firmware.model.TangoFirmwareNextFrameJson
import com.google.gson.Gson
import com.supermegazinc.escentials.waitForNextWithTimeout
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

suspend fun tangoFirmwareReceiverTest(
    incomingFirmware: Flow<ByteArray>,
    onSend: (ByteArray) -> Unit,
    logger: Logger,
    gson: Gson
) {
    val LOG_KEY = "TANGO-FW-R"

    while(coroutineContext.isActive) {

        val firmwareInit = incomingFirmware
            .mapNotNull { message ->
                try {
                    gson.fromJson(message.decodeToString(), TangoFirmwareInitJson::class.java)
                } catch (_: Exception) {
                    null
                }
            }
            .first()

        logger.i(LOG_KEY, "Recibo peticion para actualizar firmware")

        val packetQty = firmwareInit.data.packetQty

        var frameIndex = 0

        while(frameIndex<packetQty) {
            val nextFrame = TangoFirmwareNextFrameJson.TangoFirmwareNextFrameData(
                nextFrame = frameIndex+1
            )
            onSend(gson.toJson(TangoFirmwareNextFrameJson(nextFrame)).toByteArray())

            incomingFirmware.waitForNextWithTimeout(5000)

            frameIndex++
            delay(2)
        }

        logger.i(LOG_KEY, "Nuevo firmware recibido")
    }
}