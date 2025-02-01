package com.fulmar.firmware.feature_update.util

import com.fulmar.firmware.feature_update.config.TangoFirmwareUpdateConfig
import com.fulmar.firmware.feature_update.model.TangoFirmwareInitJson
import com.google.gson.Gson
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.channels.ReceiveChannel

suspend fun tangoFirmwareUpdater(
    version: String,
    onRequestFirmwareBinary: suspend () -> ByteArray?,
    onSendFirmwareInit: suspend (ByteArray) -> Boolean,
    firmwareRx: ReceiveChannel<ByteArray>,
    onSendFirmwareFrame: suspend (ByteArray) -> Boolean,
    logger: Logger,
    logKey: String
) : Boolean {
    logger.i(logKey, "Inicio actualizacion de firmware")
    logger.d(logKey, "1. Obtener el binario del firmware")
    val binary = onRequestFirmwareBinary()
    if(binary==null || binary.isEmpty()) {
        logger.e(logKey, "Error al obtener el binario")
        return false
    }
    val binarySize = binary.size
    logger.d(logKey, "Binario obtenido con exito. Tamaño: $binarySize bytes")
    logger.d(logKey, "2. Dividir el binario en paquetes de ${TangoFirmwareUpdateConfig.PACKET_SIZE_BYTES} bytes")
    val dividedFirmware = binary.dividePacket(TangoFirmwareUpdateConfig.PACKET_SIZE_BYTES)
    val packetQty =  dividedFirmware.size
    logger.d(logKey, "Binario dividido en $packetQty partes")

    logger.d(logKey, "3. Enviar solicitud de actualizacion de firmware")
    val firmwareInit = TangoFirmwareInitJson.TangoFirmwareInitData(
        version = version,
        size = binarySize,
        packetQty = packetQty
    )
    val gson = Gson()
    val serializedFirmwareInit = gson.toJson(TangoFirmwareInitJson(firmwareInit)).toByteArray()
    if(!onSendFirmwareInit(serializedFirmwareInit)) {
        logger.e(logKey, "Error al enviar la solicitud de actualizacion de firmware")
        return false
    }
    logger.d(logKey, "Solicitud de actualizacion de firmware enviada")

    logger.d(logKey, "4. Espero peticion de NextFrame y comienzo el envio del firmware")

    if(!tangoFirmwareSender(
            onSendFirmwareFrame = { onSendFirmwareFrame(it) },
            firmwareRx = firmwareRx,
            binary = dividedFirmware,
            receiveTimeoutMs = TangoFirmwareUpdateConfig.RECEIVE_TIMEOUT_MS,
            gson = gson,
            logger = logger,
            logKey = logKey
        )
    ) {
        logger.e(logKey, "No se pudo enviar el firmware")
        return false
    } else {
        return true
    }
}