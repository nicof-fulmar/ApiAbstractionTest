package com.fulmar.tango.layer1.service

import com.fulmar.tango.layer1.config.TangoL1Config
import com.fulmar.tango.trama.controllers.TramaController
import com.fulmar.tango.trama.models.Commands
import com.fulmar.tango.trama.tramas.HeaderUI
import com.fulmar.tango.trama.tramas.TramaRx
import com.fulmar.tango.trama.tramas.toTrama
import com.fulmar.tango.trama.utils.splitTrama
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import kotlin.reflect.full.createInstance

suspend fun tangoL1IncomingMessageProcessorService(
    messages: Flow<Pair<UUID, ByteArray>>,
    sharedKey: StateFlow<ByteArray?>,
    tramaController: TramaController,
    cryptographyController: CryptographyController,
    logger: Logger,
    onSendTelemetry: (ByteArray) -> Unit,
    onReceiveTelemetry: (TramaRx) -> Unit,
    onReceiveFirmware: (ByteArray) -> Unit
) {

    val LOG_KEY = "TANGO-L1-INMSG"

    messages.collect { tMessage->
        logger.d(LOG_KEY, "Mensaje recibido")

        val shared = sharedKey.value ?: run {
            logger.e(LOG_KEY, "Error al obtener la clave compartida")
            return@collect
        }

        val decrypted = cryptographyController.decrypt(
            msg = tMessage.second,
            key = shared
        ) ?: run {
            logger.e(LOG_KEY, "Error al desencriptar el mensaje")
            return@collect
        }

        logger.d(
            LOG_KEY, "Desencriptado[BYT]: ${decrypted.toList()} + " +
                    "\nDesencriptado[STR]: ${decrypted.decodeToString()}")

        when(tMessage.first) {
            TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID -> {
                val split = tramaController.splitTrama(decrypted.toList()) ?: run {
                    logger.e(LOG_KEY, "Error al separar el header de la trama, recibiendo como raw")
                    return@collect
                }

                val ack = HeaderUI(
                    packetNumber = split.first.packetNumber,
                    cmd = Commands.ACK
                )

                val serializedAck = tramaController.serialize(ack.toTrama()!!)!!.toByteArray()
                val encryptedAck = cryptographyController.encrypt(serializedAck, shared) ?: run {
                    logger.e(LOG_KEY, "Error al encriptar el ack")
                    return@collect
                }

                onSendTelemetry(encryptedAck)

                if(split.first.cmd != Commands.RELAY) return@collect

                for(subclass in TramaRx::class.sealedSubclasses) {
                    val instance = subclass.createInstance()
                    val deserializeResult = tramaController.deserialize(split.second, instance)
                    if(!deserializeResult) continue
                    onReceiveTelemetry(instance)
                    return@collect
                }
                logger.e(LOG_KEY, "No se pudo encontrar la trama correspondiente")
            }

            TangoL1Config.CHARACTERISTIC_RECEIVE_FIRMWARE, TangoL1Config.CHARACTERISTIC_RECEIVE_PROGRAMACION -> {
                onReceiveFirmware(decrypted)
            }

            else -> logger.e(LOG_KEY, "No se encontro un procedimiento para este UUID")
        }
    }
}