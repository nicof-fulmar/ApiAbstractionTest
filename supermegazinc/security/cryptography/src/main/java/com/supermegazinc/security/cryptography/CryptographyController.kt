package com.supermegazinc.security.cryptography

import android.content.Context
import com.supermegazinc.logger.Logger
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

class CryptographyController(
    private val context: Context,
    private val logger: Logger
) {

    private companion object {
        const val LOG_KEY = "CRYPT"
    }

    private val privateKey: PrivateKey

    init {
        privateKey = loadPrivateKey()
    }

    fun encrypt(msg: ByteArray, key: ByteArray): ByteArray? {
        return try {
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val secretKey = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            val encryptedData = cipher.doFinal(msg)
            iv + encryptedData
        } catch (e: Exception) {
            logger.e(LOG_KEY, "Error al encriptar: + ${e.message}")
            null
        }
    }

    fun decrypt(msg: ByteArray, key: ByteArray): ByteArray? {
        return try {
            val ivSize = 16
            if (msg.size < ivSize) return null
            val iv = msg.sliceArray(0 until ivSize)
            val encryptedData = msg.sliceArray(ivSize until msg.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val secretKey = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            cipher.doFinal(encryptedData)
        } catch (e: Exception) {
            logger.e(LOG_KEY, "Error al desencriptar: + ${e.message}")
            null
        }
    }

    fun sign(msg: ByteArray): ByteArray? {
        return try {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(msg)
            signature.sign()
        } catch (_: Exception) {
            null
        }

    }

    private fun publicKeyToHex(publicKey: PublicKey?): String {
        val encodedBytes = publicKey?.encoded
        if (encodedBytes != null) {
            return encodedBytes.joinToString("") { String.format("%02X", it) }
        }

        return ""
    }

    private fun privateKeyToHex(privateKey: PrivateKey): String {
        val encodedBytes = privateKey.encoded
        return encodedBytes.joinToString("") { String.format("%02X", it) }
    }

    /**
     * Verifica la firma de [publicKeyData] con la clave privada (leída desde raw),
     * derivando la clave pública y usando RSA + SHA-256 para la verificación.
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun verifyPublicKeySignature(
        publicKeyData: ByteArray,
        signature: ByteArray
    ): Boolean {
        return try {

            //val publicKeyDataFormat = publicKeyData.toPositiveValues()
            logger.i("criptografia","publicKeyData ${publicKeyData.toHexString()}")
            //val signatureFormat = signature.toPositiveValues()
            logger.i("criptografia","signature ${signature.toHexString()}")

            val privateKeyHex = privateKeyToHex(privateKey)
            logger.i("criptografia","privateKey $privateKeyHex")

            val publicKey = derivePublicKeyFromPrivateKey() ?: error("No se pudo derivar la clave pública")
            val publicKeyHex = publicKeyToHex(publicKey)
            logger.i("criptografia","publicKey $publicKeyHex")

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