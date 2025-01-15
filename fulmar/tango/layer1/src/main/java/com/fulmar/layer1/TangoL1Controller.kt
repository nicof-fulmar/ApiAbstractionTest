package com.fulmar.layer1

import com.fulmar.layer1.config.TangoL1Config
import com.fulmar.layer1.service.tangoL1SessionService
import com.fulmar.tango.session.TangoSessionController
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.ble_upgrade.model.BLEUpgradeDisconnectReason
import com.supermegazinc.escentials.firstWithTimeout
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.util.UUID

class TangoL1Controller(
    private val bleUpgradeController: BLEUpgradeController,
    private val cryptographyController: CryptographyController,
    private val tangoSessionController: TangoSessionController,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TANGO-L1"
    }

    private val _status = MutableStateFlow<BLEUpgradeConnectionStatus>(BLEUpgradeConnectionStatus.Disconnected(BLEUpgradeDisconnectReason.BLE(BLEDisconnectionReason.DISCONNECTED)))
    val status = _status.asStateFlow()

    private var newSessionJob: Job? = null
    private fun newSession() {
        newSessionJob?.cancel()
        newSessionJob = coroutineScope.launch {
            try {
                logger.i(LOG_KEY, "[SES] - Generando sesion..")
                val publicKey = tangoSessionController.refreshAndGetPublicKey().toByteArray()

                val publicKeySigned = cryptographyController.sign(publicKey)

                if(publicKeySigned == null) {
                    logger.e(LOG_KEY, "[SES] - No se pudo firmar la clave publica")
                    return@launch
                }

                val publicKeySignedFull = publicKey + publicKeySigned

                logger.d(LOG_KEY,"[SES] - Buscando caracteristica 'ReceivePublicKey'..")
                val receiveCharacteristic = bleUpgradeController
                    .characteristics
                    .map { it.firstOrNull {char-> TangoL1Config.CHARACTERISTIC_RECEIVE_KEY_UUID == char.uuid} }
                    .filterNotNull()
                    .firstWithTimeout(5000)
                logger.d(LOG_KEY,"[SES] - Encontrada, solicitando clave publica..")

                receiveCharacteristic.forceRead()
                val peerPublicKey = receiveCharacteristic.message
                    .filterNotNull()
                    .firstWithTimeout(5000)
                logger.d(LOG_KEY,"[SES] - Encontrada, clave recibida [${peerPublicKey.size}]: ${peerPublicKey.toList()}")

                if(!cryptographyController.verifyPublicKeySignature(peerPublicKey.copyOfRange(0,65),peerPublicKey.copyOfRange(65,65+256))) {
                    logger.e(LOG_KEY, "[SES] - Verificacion de clave publica fallida")
                    return@launch
                } else {
                    logger.d(LOG_KEY, "[SES] - Verificacion de clave publica correcta")
                }

                logger.d(LOG_KEY,"[SES] - Buscando caracteristica 'SendPublicKey'..")
                val sendCharacteristic = bleUpgradeController
                    .characteristics
                    .mapNotNull { it.firstOrNull {char-> TangoL1Config.CHARACTERISTIC_SEND_KEY_UUID == char.uuid} }
                    .filterNotNull()
                    .firstWithTimeout(5000)
                logger.d(LOG_KEY,"[SES] - Encontrada, enviando clave publica [${publicKey.size}]: ${publicKey.toList()}")

                sendCharacteristic.send(publicKeySignedFull)

                logger.d(LOG_KEY,"[SES] - Calculando clave compartida..")

                val session = tangoSessionController.generateSession(peerPublicKey.copyOfRange(0,65).toList())

                if(session==null) {
                    logger.e(LOG_KEY, "[SES] - No se pudo calcular la clave compartida")
                    return@launch
                }

                logger.i(LOG_KEY,"[SES] - Sesion generada con exito\n" +
                        "PUBLIC[${session.myPublicKey.size}]: ${session.myPublicKey}\n" +
                        "SHARED[${session.sharedKey.size}]: ${session.sharedKey}"
                )

            } catch (e: CancellationException) {
                logger.e(LOG_KEY, "[SES] - Cancelado")
            }
        }
    }

    init {
        coroutineScope.launch {
            tangoL1SessionService(
                bleUpgradeController.status,
                onNewSession = {
                    logger.i(LOG_KEY, "[SES-SERVICE] - onNewSession")
                    newSession()
                },
                onEndSession = {
                    logger.i(LOG_KEY, "[SES-SERVICE] - onEndSession")
                    newSessionJob?.cancel()
                }
            )
        }
    }

}