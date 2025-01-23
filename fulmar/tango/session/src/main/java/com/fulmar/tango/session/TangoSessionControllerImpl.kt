package com.fulmar.tango.session

import com.fulmar.tango.session.model.TangoSession
import com.supermegazinc.escentials.Status
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.diffie_hellman.DiffieHellmanController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
        _session.update { Status.None() }
        diffieHellmanController.refreshKey()
        return diffieHellmanController.myPublicKeyBytes
    }

    override suspend fun generateSession(peerPublicKey: List<Byte>): TangoSession? {
        logger.d(LOG_KEY,"[GEN_SES] - Generando sesion..")
        _session.update { Status.None() }
        val sharedKey = diffieHellmanController.sharedKey(peerPublicKey) ?: run {
            logger.e(LOG_KEY,"[GEN_SES] - No se pudo obtener la clave compartida")
            return null
        }

        val session = TangoSession(
            myPublicKey = diffieHellmanController.myPublicKeyBytes,
            peerPublicKey = peerPublicKey,
            sharedKey = sharedKey
        )

        logger.i(LOG_KEY,"[GEN_SES] - Sesion generada")
        _session.update { Status.Ready(session) }
        return session
    }
}