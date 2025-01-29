package com.example.apiabstractiontest.ble_test

import android.content.Context
import com.supermegazinc.cryptography.R
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptographyControllerTestImpl(
    private val context: Context,
    private val logger: Logger,
): CryptographyController {

    private val privateKey: PrivateKey

    init {
        privateKey = loadPrivateKey()
    }

    override fun encrypt(msg: ByteArray, key: ByteArray): ByteArray {
        return msg
    }

    override fun decrypt(msg: ByteArray, key: ByteArray): ByteArray {
        return msg
    }

    override fun sign(msg: ByteArray): ByteArray? {
        return try {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(msg)
            signature.sign()
        } catch (_: Exception) {
            null
        }

    }

    override fun verifyPublicKeySignature(
        publicKeyData: ByteArray,
        signature: ByteArray
    ): Boolean {
        return try {
            val publicKey = derivePublicKeyFromPrivateKey() ?: error("No se pudo derivar la clave pública")
            val signatureAlg = Signature.getInstance("SHA256withRSA")
            signatureAlg.initVerify(publicKey)
            signatureAlg.update(publicKeyData)
            signatureAlg.verify(signature)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Load private key from res/raw/key.der
    private fun loadPrivateKey(): PrivateKey {
        val keyBytes = context.resources.openRawResource(R.raw.private_key_pkcs1).use { it.readBytes() }
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(spec)
    }

    private fun derivePublicKeyFromPrivateKey(): PublicKey? {
        return try {
            val kf = KeyFactory.getInstance("RSA")
            val rsaPrivkSpec = kf.getKeySpec(privateKey, RSAPrivateCrtKeySpec::class.java) as RSAPrivateCrtKeySpec
            val pubSpec = RSAPublicKeySpec(rsaPrivkSpec.modulus, rsaPrivkSpec.publicExponent)
            kf.generatePublic(pubSpec)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}