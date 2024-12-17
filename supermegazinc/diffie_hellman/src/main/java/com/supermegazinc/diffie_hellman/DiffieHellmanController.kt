package com.supermegazinc.diffie_hellman

import com.supermegazinc.logger.Logger
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
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

    lateinit var myPublicKeyBytes: ByteArray
        private set

    private lateinit var myKey: AsymmetricCipherKeyPair

    fun refreshKey() {
        logger.d(LOG_KEY, "Actualizando clave..")
        val curveName = "secp256r1"
        val ecParameterSpec = SECNamedCurves.getByName(curveName)
        val domain = ECDomainParameters(
            ecParameterSpec.curve,
            ecParameterSpec.g,
            ecParameterSpec.n,
            ecParameterSpec.h
        )

        val keyGenParam = ECKeyGenerationParameters(domain, SecureRandom())
        val keyPairGenerator = ECKeyPairGenerator()
        keyPairGenerator.init(keyGenParam)

        myKey = keyPairGenerator.generateKeyPair()
        val myPublicKeyParams = myKey.public as ECPublicKeyParameters

        myPublicKeyBytes = myPublicKeyParams.q.getEncoded(false)
    }

    init {
        refreshKey()
    }


    fun sharedKey(peerPublicKeyBytes: ByteArray): ByteArray? {
        logger.d(LOG_KEY, "Calculando clave compartida..")
        return try {
            val curveName = "secp256r1"
            val ecParameterSpec = SECNamedCurves.getByName(curveName)
            val domain = ECDomainParameters(
                ecParameterSpec.curve,
                ecParameterSpec.g,
                ecParameterSpec.n,
                ecParameterSpec.h
            )

            val q = ecParameterSpec.curve.decodePoint(peerPublicKeyBytes)
            val peerPublicKeyParams = ECPublicKeyParameters(q, domain)

            val myPrivateKeyParams = myKey.private as ECPrivateKeyParameters

            val agreement = ECDHBasicAgreement()
            agreement.init(myPrivateKeyParams)

            val sharedSecret = agreement.calculateAgreement(peerPublicKeyParams)
            sharedSecret.toByteArray()

            /*
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

             */
        } catch (_: Exception) {
            logger.e(LOG_KEY, "ERROR al calcular clave compartida")
            null
        }
    }

}