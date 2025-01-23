package com.fulmar.tango.layer1.feature_incoming_message

import com.fulmar.tango.trama.controllers.TramaController
import com.fulmar.tango.trama.models.Commands
import com.fulmar.tango.trama.tramas.HeaderUI
import com.fulmar.tango.trama.tramas.TramaRx
import com.fulmar.tango.trama.tramas.toTrama
import com.fulmar.tango.trama.utils.splitTrama
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.full.createInstance

class TangoL1IncomingMessageProcessor(
    private val onSendTelemetry: (ByteArray) -> Unit,
    private val onReceiveTelemetry: (TramaRx) -> Unit,
    private val onReceiveFirmware: (ByteArray) -> Unit,
    private val tramaController: TramaController,
    private val cryptographyController: CryptographyController,
    private val sharedKey: StateFlow<ByteArray?>,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TANGO-L1-INMSG"
    }

    fun onReceiveRawTelemetry(message: ByteArray) = coroutineScope.launch {
        logger.d(LOG_KEY, "onReceiveRawTelemetry")

        val shared = sharedKey.value ?: run {
            logger.e(LOG_KEY, "Error al obtener la clave compartida")
            return@launch
        }

        val decrypted = cryptographyController.decrypt(
            msg = message,
            key = shared
        ) ?: run {
            logger.e(LOG_KEY, "Error al desencriptar el mensaje")
            return@launch
        }

        val split = tramaController.splitTrama(decrypted.toList()) ?: run {
            logger.e(LOG_KEY, "Error al separar el header de la trama, recibiendo como raw")
            return@launch
        }

        val ack = HeaderUI(
            packetNumber = split.first.packetNumber,
            cmd = Commands.ACK
        )

        logger.d(LOG_KEY, "Enviando ACK (paquete #${split.first.packetNumber})")

        val serializedAck = tramaController.serialize(ack.toTrama()!!)!!.toByteArray()
        val encryptedAck = cryptographyController.encrypt(serializedAck, shared) ?: run {
            logger.e(LOG_KEY, "Error al encriptar el ack")
            return@launch
        }

        onSendTelemetry(encryptedAck)

        for(subclass in TramaRx::class.sealedSubclasses) {
            val instance = subclass.createInstance()
            val deserializeResult = tramaController.deserialize(split.second, instance)
            if(!deserializeResult) continue
            onReceiveTelemetry(instance)
            return@launch
        }
        logger.e(LOG_KEY, "No se pudo encontrar la trama correspondiente")

    }

    fun onReceiveRawFirmware(message: ByteArray) = coroutineScope.launch{
        //logger.d(LOG_KEY, "onReceiveRawFirmware")

        val shared = sharedKey.value ?: run {
            logger.e(LOG_KEY, "Error al obtener la clave compartida")
            return@launch
        }

        val decrypted = cryptographyController.decrypt(
            msg = message,
            key = shared
        ) ?: run {
            logger.e(LOG_KEY, "Error al desencriptar el mensaje")
            return@launch
        }

        onReceiveFirmware(decrypted)
    }

}