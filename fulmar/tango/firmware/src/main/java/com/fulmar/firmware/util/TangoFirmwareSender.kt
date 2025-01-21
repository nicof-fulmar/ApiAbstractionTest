package com.fulmar.firmware.util

import com.fulmar.firmware.model.TangoFirmwareNextFrameJson
import com.google.gson.Gson
import com.supermegazinc.escentials.waitForNextWithTimeout
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow

suspend fun tangoFirmwareSender(
    onSendFirmwareTx: suspend (ByteArray) -> Boolean,
    firmwareRx: Flow<ByteArray>,
    binary: List<ByteArray>,
    receiveTimeoutMs: Long,
    gson: Gson,
    logger: Logger,
    LOG_KEY: String
): Boolean {

    var lastFrameSent = 0

    while(lastFrameSent<binary.size) {
        val expectedFrame = lastFrameSent+1

        val incomingMsg = try {
            firmwareRx.waitForNextWithTimeout(receiveTimeoutMs)
        } catch (_: TimeoutCancellationException) {
            logger.e(LOG_KEY, "Timeout: No recibi nextFrame con frame: $expectedFrame")
            return false
        }

        val deserializedFirmwareNextFrame = try {
            gson.fromJson(incomingMsg.decodeToString(), TangoFirmwareNextFrameJson::class.java)!!
        } catch (_: Exception) {
            logger.e(LOG_KEY, "Error al deserializar json")
            return false
        }

        val requestedFrame = deserializedFirmwareNextFrame.data.nextFrame
        if(requestedFrame != expectedFrame) {
            logger.e(LOG_KEY, "Esperaba frame $expectedFrame pero recibi $requestedFrame")
            return false
        }

        logger.d(LOG_KEY, "Enviando frame #$expectedFrame (${lastFrameSent*100/binary.size}%)")
        if(!onSendFirmwareTx(binary[lastFrameSent])) {
            logger.e(LOG_KEY,"No se pudo enviar el frame")
            return false
        }

        lastFrameSent+=1
    }

    return true

}