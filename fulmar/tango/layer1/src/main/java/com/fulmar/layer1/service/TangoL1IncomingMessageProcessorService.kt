package com.fulmar.layer1.service

import com.fulmar.tango.trama.controllers.TramaController
import com.fulmar.tango.trama.controllers.TramaControllerImpl
import com.fulmar.tango.trama.models.Commands
import com.fulmar.tango.trama.tramas.HeaderUI
import com.fulmar.tango.trama.tramas.TramaRx
import com.fulmar.tango.trama.tramas.toTrama
import com.fulmar.tango.trama.utils.splitTrama
import com.supermegazinc.escentials.Status
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.log
import kotlin.reflect.full.createInstance

suspend fun tangoL1IncomingMessageProcessorService(
    messages: Flow<ByteArray>,
    sharedKey: StateFlow<ByteArray?>,
    tramaController: TramaController,
    cryptographyController: CryptographyController,
    logger: Logger,
    onSend: (ByteArray) -> Unit,
    onReceive: (TramaRx) -> Unit
) {

    val LOG_KEY = "TANGO-L1-INMSG"

    messages.collect { tMessage->
        logger.d(LOG_KEY, "Mensaje recibido")

        val shared = sharedKey.value ?: run {
            logger.e(LOG_KEY, "Error al obtener la clave compartida")
            return@collect
        }

        val decrypted = cryptographyController.decrypt(
            msg = tMessage,
            key = shared
        ) ?: run {
            logger.e(LOG_KEY, "Error al desencriptar el mensaje")
            return@collect
        }

        logger.d(
            LOG_KEY, "Desencriptado[BYT]: ${decrypted.toList()} + " +
                    "\nDesencriptado[STR]: ${decrypted.decodeToString()}")

        val split = tramaController.splitTrama(decrypted.toList()) ?: run {
            logger.e(LOG_KEY, "Error al separar el header de la trama")
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

        onSend(encryptedAck)

        if(split.first.cmd != Commands.RELAY) return@collect

        for(subclass in TramaRx::class.sealedSubclasses) {
            val instance = subclass.createInstance()
            val deserializeResult = tramaController.deserialize(split.second, instance)
            if(!deserializeResult) continue
            onReceive(instance)
        }
    }
}