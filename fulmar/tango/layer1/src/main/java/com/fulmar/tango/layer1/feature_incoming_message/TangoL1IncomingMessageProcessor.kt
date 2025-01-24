package com.fulmar.tango.layer1.feature_incoming_message

import com.fulmar.tango.layer1.models.TangoL1Telemetry
import com.fulmar.tango.trama.controllers.TramaController
import com.fulmar.tango.trama.models.Commands
import com.fulmar.tango.trama.tramas.HeaderUI
import com.fulmar.tango.trama.tramas.ScabRx
import com.fulmar.tango.trama.tramas.SimpRx
import com.fulmar.tango.trama.tramas.TramaRx
import com.fulmar.tango.trama.tramas.toTicketUI
import com.fulmar.tango.trama.tramas.toTrama
import com.fulmar.tango.trama.tramas.toUI
import com.fulmar.tango.trama.utils.splitTrama
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.full.createInstance

class TangoL1IncomingMessageProcessor(
    private val firmwareRaw: ReceiveChannel<ByteArray>,
    private val telemetryRaw: ReceiveChannel<ByteArray>,
    private val onSendAck: (ByteArray) -> Unit,
    private val sharedKey: StateFlow<ByteArray?>,
    private val tramaController: TramaController,
    private val cryptographyController: CryptographyController,
    private val logger: Logger,
    coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TANGO-L1-INMSG"
    }

    private val _currentTelemetry = MutableStateFlow(TangoL1Telemetry())
    val currentTelemetry = _currentTelemetry.asStateFlow()

    private val _firmware = Channel<ByteArray>(capacity = 100, onBufferOverflow = BufferOverflow.SUSPEND)
    val firmware: ReceiveChannel<ByteArray> = _firmware

    init {
        coroutineScope.launch {
            firmwareRaw.receiveAsFlow().collect { message->
                val shared = sharedKey.value ?: run {
                    logger.e(LOG_KEY, "Error al obtener la clave compartida")
                    return@collect
                }

                val decrypted = cryptographyController.decrypt(
                    msg = message,
                    key = shared
                ) ?: run {
                    logger.e(LOG_KEY, "Error al desencriptar el mensaje")
                    return@collect
                }

                _firmware.send(decrypted)

            }
        }
        coroutineScope.launch {
            telemetryRaw.receiveAsFlow().collect { message->
                logger.d(LOG_KEY, "onReceiveRawTelemetry")

                val shared = sharedKey.value ?: run {
                    logger.e(LOG_KEY, "Error al obtener la clave compartida")
                    return@collect
                }

                val decrypted = cryptographyController.decrypt(
                    msg = message,
                    key = shared
                ) ?: run {
                    logger.e(LOG_KEY, "Error al desencriptar el mensaje")
                    return@collect
                }

                val split = tramaController.splitTrama(decrypted.toList()) ?: run {
                    logger.e(LOG_KEY, "Error al separar el header de la trama, recibiendo como raw")
                    return@collect
                }

                val ack = HeaderUI(
                    packetNumber = split.first.packetNumber,
                    cmd = Commands.ACK
                )

                logger.d(LOG_KEY, "Enviando ACK (paquete #${split.first.packetNumber})")

                val serializedAck = tramaController.serialize(ack.toTrama()!!)!!.toByteArray()
                val encryptedAck = cryptographyController.encrypt(serializedAck, shared) ?: run {
                    logger.e(LOG_KEY, "Error al encriptar el ack")
                    return@collect
                }

                onSendAck(encryptedAck)

                if(split.first.cmd == Commands.RELAY) {
                    for(subclass in TramaRx::class.sealedSubclasses) {
                        val instance = subclass.createInstance()
                        val deserializeResult = tramaController.deserialize(split.second, instance)
                        if (!deserializeResult) continue
                        when (instance) {
                            is ScabRx -> {
                                _currentTelemetry.update { it.copy(scab = instance.toUI()) }
                            }
                            is SimpRx -> {
                                _currentTelemetry.update {
                                    it.copy(
                                        simpSummary = instance.toUI(),
                                        simpTicket = instance.toTicketUI()
                                    )
                                }
                            }
                        }
                        return@collect
                    }
                    logger.e(LOG_KEY, "[CRITIC] - No se pudo encontrar la trama correspondiente")
                } else {
                    return@collect
                }
            }
        }
    }

}