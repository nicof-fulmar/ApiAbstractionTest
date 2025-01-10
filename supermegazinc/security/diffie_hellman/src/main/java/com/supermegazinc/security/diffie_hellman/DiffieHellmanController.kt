package com.supermegazinc.security.diffie_hellman

import com.supermegazinc.logger.Logger
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import java.security.PrivateKey
import java.security.SecureRandom

class DiffieHellmanController(
    private val logger: Logger
) {

    private companion object{
        private const val LOG_KEY = "DIFHELL"
    }

    lateinit var myPublicKeyBytes: List<Byte>
        private set

    lateinit var myPrivateKey: PrivateKey
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
        val myPrivateKeyParams = myKey.private as ECPrivateKeyParameters

        myPublicKeyBytes = myPublicKeyParams.q.getEncoded(false).toList()
        myPrivateKey = myKey.private as PrivateKey
    }

    init {
        refreshKey()
    }


    fun sharedKey(peerPublicKeyBytes: List<Byte>): List<Byte>? {
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

            val q = ecParameterSpec.curve.decodePoint(peerPublicKeyBytes.toByteArray())
            val peerPublicKeyParams = ECPublicKeyParameters(q, domain)

            val myPrivateKeyParams = myKey.private as ECPrivateKeyParameters

            val agreement = ECDHBasicAgreement()
            agreement.init(myPrivateKeyParams)

            val sharedSecret = agreement.calculateAgreement(peerPublicKeyParams)
            sharedSecret.toByteArray().toList()
        } catch (_: Exception) {
            logger.e(LOG_KEY, "ERROR al calcular clave compartida")
            null
        }
    }

}