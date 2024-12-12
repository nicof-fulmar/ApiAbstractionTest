package com.supermegazinc.diffie_hellman

import com.supermegazinc.logger.Logger
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.KeyAgreement

class DiffieHellmanController(
    private val logger: Logger
) {

    private companion object{
        private const val LOG_KEY = "DIFHELL"
    }

    lateinit var myPublicKey: ECPublicKey
        private set

    private lateinit var myPrivateKey: PrivateKey

    private val ecSpec = ECGenParameterSpec("secp256r1")
    private val keyPairGenerator by lazy {
        KeyPairGenerator.getInstance("EC").also {
            it.initialize(ecSpec, SecureRandom())
        }
    }

    fun refreshKey() {
        logger.d(LOG_KEY, "Actualizando clave..")
        val keyPair = keyPairGenerator.generateKeyPair()
        myPrivateKey = keyPair.private
        myPublicKey = keyPair.public as ECPublicKey
    }

    fun sharedKey(peerPublicKeyBytes: ByteArray): ByteArray? {
        logger.d(LOG_KEY, "Calculando clave compartida..")
        return try {
            val xBytes = peerPublicKeyBytes.sliceArray(1 until 33)
            val yBytes = peerPublicKeyBytes.sliceArray(33 until 65)
            val point = ECPoint(BigInteger(1, xBytes), BigInteger(1, yBytes))
            val ecParams = myPublicKey.params
            val peerPublicKeySpec = ECPublicKeySpec(point, ecParams)
            val keyFactory = KeyFactory.getInstance("EC")
            val peerPublicKey = keyFactory.generatePublic(peerPublicKeySpec)
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(myPrivateKey)
            keyAgreement.doPhase(peerPublicKey, true)
            keyAgreement.generateSecret()
        } catch (_: Exception) {
            logger.e(LOG_KEY, "ERROR al calcular clave compartida")
            null
        }
    }

    init {
        refreshKey()
    }
}