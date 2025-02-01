package com.fulmar.firmware.feature_update.util

import com.fulmar.firmware.feature_update.model.TangoFirmwareNextFrameJson
import com.google.gson.Gson
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withTimeout

suspend fun tangoFirmwareSender(
    onSendFirmwareFrame: suspend (ByteArray) -> Boolean,
    firmwareRx: ReceiveChannel<ByteArray>,
    binary: List<ByteArray>,
    receiveTimeoutMs: Long,
    gson: Gson,
    logger: Logger,
    logKey: String,
): Boolean {

    var lastFrameSent = 0

    while(lastFrameSent<binary.size) {
        val expectedFrame = lastFrameSent+1

        val incomingMsg = try {
            withTimeout(receiveTimeoutMs) {
                firmwareRx.receive()
            }
        } catch (_: TimeoutCancellationException) {
            logger.e(logKey, "Timeout: No recibi nextFrame con frame: $expectedFrame")
            return false
        }

        val deserializedFirmwareNextFrame = try {
            gson.fromJson(incomingMsg.decodeToString(), TangoFirmwareNextFrameJson::class.java)!!
        } catch (_: Exception) {
            logger.e(logKey, "Error al deserializar json:\n[BYT]:${incomingMsg.toList()}\n[STR]:'${incomingMsg.decodeToString()}'")
            return false
        }

        val requestedFrame = deserializedFirmwareNextFrame.data.nextFrame

        val frameToSend = if(expectedFrame>1 && requestedFrame == expectedFrame-1) {
            logger.e(logKey, "El dispositivo volvio a pedir el frame ${expectedFrame-1}, ojo..")
            expectedFrame-1
        } else if(requestedFrame != expectedFrame) {
            logger.e(logKey, "Esperaba frame $expectedFrame pero recibi $requestedFrame")
            return false
        } else {
            expectedFrame
        }

        logger.d(logKey, "Enviando frame #$frameToSend (${frameToSend*100/binary.size}%)")
        if(!onSendFirmwareFrame(binary[frameToSend-1])) {
            logger.e(logKey,"No se pudo enviar el frame")
            return false
        }

        lastFrameSent = frameToSend
    }

    return true

}