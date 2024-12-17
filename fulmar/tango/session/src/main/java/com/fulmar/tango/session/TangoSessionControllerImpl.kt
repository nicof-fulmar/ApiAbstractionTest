package com.fulmar.tango.session

import com.fulmar.tango.session.model.TangoSession
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.diffie_hellman.DiffieHellmanController
import com.supermegazinc.escentials.Status
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class TangoSessionControllerImpl(
    private val bleUpgradeController: BLEUpgradeController,
    private val receivePublicKeyCharacteristicUUID: UUID,
    private val sendPublicKeyCharacteristicUUID: UUID,
    private val diffieHellmanController: DiffieHellmanController,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) : TangoSessionController {

    private companion object {
        private const val LOG_KEY = "TANGO-SESSION"
    }

    private val _session = MutableStateFlow<Status<TangoSession>>(Status.None())
    override val session: StateFlow<Status<TangoSession>>
        get() = _session.asStateFlow()

    private var generateSessionJob: Job? = null
    private fun generateSession() {
        generateSessionJob?.cancel()
        generateSessionJob = coroutineScope.launch {
            try {
                diffieHellmanController.refreshKey()

                logger.d(LOG_KEY,"Buscando caracteristica 'SendPublicKey'..")
                val sendCharacteristic = bleUpgradeController
                    .characteristics
                    .map { it.firstOrNull {char-> sendPublicKeyCharacteristicUUID == char.uuid} }
                    .filterNotNull()
                    .first()
                logger.d(LOG_KEY,"Encontrada, enviando clave publica: [${diffieHellmanController.myPublicKeyBytes.size}]: ${diffieHellmanController.myPublicKeyBytes.toList()}")

                sendCharacteristic.send(diffieHellmanController.myPublicKeyBytes)

                logger.d(LOG_KEY,"Buscando caracteristica 'ReceivePublicKey'..")
                val receiveCharacteristic = bleUpgradeController
                    .characteristics
                    .map { it.firstOrNull {char-> receivePublicKeyCharacteristicUUID == char.uuid} }
                    .filterNotNull()
                    .first()
                receiveCharacteristic.forceRead()
                val key = receiveCharacteristic.message.filterNotNull().first()
                logger.d(LOG_KEY,"Encontrada, clave recibida: [${key.size}]: ${key.toList()}")

                val sharedKey = diffieHellmanController.sharedKey(key) ?: return@launch

                logger.d(LOG_KEY,"Clave compartida: [${sharedKey.size}]: ${sharedKey.toList()}")

                logger.d(LOG_KEY,"Sesion generada con exito")
            } catch (e: CancellationException) {
                logger.e(LOG_KEY,"Generacion de sesion cancelada")
            }
        }
    }

    init {
        coroutineScope.launch {
            bleUpgradeController.status.collectLatest { status->
                when(status) {
                    is BLEUpgradeConnectionStatus.Connected -> {
                        logger.i(LOG_KEY,"Conexión detectada, intentando generar sesion..")
                        generateSession()
                    }
                    else -> {}
                }
            }
        }
    }
}