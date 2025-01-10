package com.fulmar.tango.session

import com.fulmar.tango.session.model.TangoSession
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.security.diffie_hellman.DiffieHellmanController
import com.supermegazinc.escentials.Result
import com.supermegazinc.escentials.Status
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class TangoSessionControllerImpl(
    private val logger: Logger
) : TangoSessionController {

    private companion object {
        private const val LOG_KEY = "TANGO-SESSION"
    }

    private val diffieHellmanController: DiffieHellmanController = DiffieHellmanController(logger)

    private val _session = MutableStateFlow<Status<TangoSession>>(Status.None())
    override val session: StateFlow<Status<TangoSession>>
        get() = _session.asStateFlow()

    override suspend fun refreshAndGetPublicKey(): List<Byte> {
        logger.d(LOG_KEY,"Actualizando y obteniendo clave publica..")
        diffieHellmanController.refreshKey()
        return diffieHellmanController.myPublicKeyBytes
    }

    override suspend fun generateSession(peerPublicKey: List<Byte>): Result<TangoSession, Unit> {
        logger.d(LOG_KEY,"[GEN_SES] - Generando sesion..")
        _session.update { Status.None() }
        val sharedKey = diffieHellmanController.sharedKey(peerPublicKey) ?: run {
            logger.e(LOG_KEY,"[GEN_SES] - No se pudo obtener la clave compartida")
            return Result.Fail(Unit)
        }

        val session = TangoSession(
            myPrivateKey = diffieHellmanController.myPrivateKey,
            myPublicKey = diffieHellmanController.myPublicKeyBytes,
            peerPublicKey = peerPublicKey,
            sharedKey = sharedKey
        )

        logger.i(LOG_KEY,"[GEN_SES] - Sesion generada")
        _session.update { Status.Ready(session) }
        return Result.Success(session)
    }

    /*
        override suspend fun generateSession(): Result<TangoSession, Unit> {

        logger.d(LOG_KEY,"Buscando caracteristica 'SendPublicKey'..")
        val sendCharacteristic = bleUpgradeController
            .characteristics
            .map { it.firstOrNull {char-> sendPublicKeyCharacteristicUUID == char.uuid} }
            .filterNotNull()
            .first()
        logger.d(LOG_KEY,"Encontrada, enviando clave publica: [${diffieHellmanController.myPublicKeyBytes.size}]: ${diffieHellmanController.myPublicKeyBytes.toList()}")

        sendCharacteristic.send(diffieHellmanController.myPublicKeyBytes.toByteArray())

        logger.d(LOG_KEY,"Buscando caracteristica 'ReceivePublicKey'..")
        val receiveCharacteristic = bleUpgradeController
            .characteristics
            .map { it.firstOrNull {char-> receivePublicKeyCharacteristicUUID == char.uuid} }
            .filterNotNull()
            .first()
        receiveCharacteristic.forceRead()
        val peerPublicKey = receiveCharacteristic.message.filterNotNull().first().toList()
        logger.d(LOG_KEY,"Encontrada, clave recibida: [${peerPublicKey.size}]: ${peerPublicKey.toList()}")

        val sharedKey = diffieHellmanController.sharedKey(peerPublicKey) ?: return Result.Fail(Unit)

        logger.d(LOG_KEY,"Clave compartida: [${sharedKey.size}]: ${sharedKey.toList()}")

        logger.d(LOG_KEY,"Sesion generada con exito")


    }
     */

}