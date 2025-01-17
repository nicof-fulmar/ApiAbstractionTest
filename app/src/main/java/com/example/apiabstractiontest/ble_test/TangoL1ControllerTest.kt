package com.example.apiabstractiontest.ble_test

import androidx.compose.runtime.collectAsState
import com.fulmar.layer1.service.tangoL1IncomingMessageProcessorService
import com.fulmar.layer1.service.tangoL1SessionService
import com.fulmar.tango.session.TangoSessionController
import com.fulmar.tango.trama.controllers.TramaControllerImpl
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.escentials.Status
import com.supermegazinc.escentials.firstWithTimeout
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.log

class TangoL1ControllerTest(
    private val bleUpgradeController: BLEUpgradeController,
    private val tangoSessionController: TangoSessionController,
    private val cryptographyController: CryptographyController,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope,
) {

    private companion object {
        const val LOG_KEY = "TANGO-L1"
    }

    private var newSessionJob: Job? = null
    private fun newSession() {
        newSessionJob?.cancel()
        newSessionJob = coroutineScope.launch {
            try {
                logger.i(LOG_KEY,"Generando sesion..")

                val publicKey = tangoSessionController.refreshAndGetPublicKey().toByteArray()

                val publicKeySigned = cryptographyController.sign(publicKey)

                if(publicKeySigned == null) {
                    logger.e(LOG_KEY, "No se pudo firmar la clave publica")
                    return@launch
                }

                val publicKeySignedFull = publicKey + publicKeySigned

                logger.d(LOG_KEY,"Buscando caracteristica 'ReceivePublicKey'..")
                val receiveCharacteristic = bleUpgradeController
                    .characteristics
                    .map { it.firstOrNull {char-> UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9") == char.uuid} }
                    .filterNotNull()
                    .firstWithTimeout(5000)
                logger.d(LOG_KEY,"Encontrada, solicitando clave publica..")

                receiveCharacteristic.forceRead()
                val peerPublicKey = receiveCharacteristic.message
                    .filterNotNull()
                    .firstWithTimeout(5000)
                logger.d(LOG_KEY,"Encontrada, clave recibida [${peerPublicKey.size}]: ${peerPublicKey.toList()}")

                if(!cryptographyController.verifyPublicKeySignature(peerPublicKey.copyOfRange(0,65),peerPublicKey.copyOfRange(65,65+256))) {
                    logger.e(LOG_KEY, "Verificacion de clave publica fallida")
                    return@launch
                } else {
                    logger.d(LOG_KEY, "Verificacion de clave publica correcta")
                }

                logger.d(LOG_KEY,"Buscando caracteristica 'SendPublicKey'..")
                val sendCharacteristic = bleUpgradeController
                    .characteristics
                    .mapNotNull { it.firstOrNull {char-> UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa") == char.uuid} }
                    .filterNotNull()
                    .firstWithTimeout(5000)
                logger.d(LOG_KEY,"Encontrada, enviando clave publica [${publicKey.size}]: ${publicKey.toList()}")

                sendCharacteristic.send(publicKeySignedFull)

                logger.d(LOG_KEY,"Calculando clave compartida..")

                val session = tangoSessionController.generateSession(peerPublicKey.copyOfRange(0,65).toList())

                if(session==null) {
                    logger.e(LOG_KEY, "No se pudo calcular la clave compartida")
                    return@launch
                }

                logger.i(LOG_KEY,"Sesion generada con exito\n" +
                        "PUBLIC[${session.myPublicKey.size}]: ${session.myPublicKey}\n" +
                        "SHARED[${session.sharedKey.size}]: ${session.sharedKey}"
                        )


            } catch (e: TimeoutCancellationException) {
                logger.e(LOG_KEY, "Timeout")
            }
        }
    }

    private val incomingMessages = MutableSharedFlow<ByteArray>()

    init {

        coroutineScope.launch {
            tangoL1SessionService(
                connectionStatus = bleUpgradeController.status,
                onNewSession = {
                    newSession()
                },
                onEndSession = {
                    newSessionJob?.cancel()
                }
            )
        }

        coroutineScope.launch {
            tangoL1IncomingMessageProcessorService(
                messages = incomingMessages,
                tramaController = TramaControllerImpl(),
                cryptographyController = cryptographyController,
                sharedKey = tangoSessionController.session.map { (it as? Status.Ready)?.data?.sharedKey?.toByteArray() }.stateIn(coroutineScope),
                //sharedKey = MutableStateFlow(BLETestK.TANGO_SHARED_KEY)
                logger = logger,
                onSend = { message->
                    logger.i(LOG_KEY, "Enviando: \n${message.toList()}")
                    launch {
                        bleUpgradeController.characteristics
                            .value
                            .firstOrNull { it.uuid == BLETestK.CHARACTERISTIC_SEND_TELEMETRY_UUID }
                            ?.send(message)
                    }
                },
                onReceive = {trama->
                    logger.i(LOG_KEY, "Recibido: \n${trama}")
                }
            )
        }

        coroutineScope.launch {
            coroutineScope {
                bleUpgradeController
                    .status
                    .filterIsInstance<BLEUpgradeConnectionStatus.Connected>()
                    .collectLatest { _->
                        bleUpgradeController
                            .characteristics
                            .mapNotNull { characteristics->
                                characteristics.firstOrNull {
                                    it.uuid == BLETestK.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID
                                }
                            }.collectLatest { characteristic->

                                characteristic.setNotification(true)

                                characteristic
                                    .message
                                    .filterNotNull()
                                    .collect { incomingMsg->
                                        incomingMessages.emit(incomingMsg)
                                    }
                            }
                    }
            }
        }
    }
}