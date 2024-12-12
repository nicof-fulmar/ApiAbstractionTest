package com.example.apiabstractiontest

import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

fun main() {

    val peerPublicKeyBytes = listOf<Byte>(
        4, -78, 37, 2, -107, -111, -81, -31, -30, 127, 90, -88, -91, -46, 80, -107, -72, -112, 68, -61, 47, -53, 63, -122, -35, 12, -92, 108, 124, -31, 92, -62, 92, -95, -116, -81, 81, 124, 19, 48, -115, -41, 106, -13, 20, 25, 111, 114, 83, 57, -14, 89, 107, 71, -52, -44, -70, -35, -24, -108, -102, 121, -15, -112, 82
    ).toByteArray()


    println("Clave compartida: ${sharedSecret.joinToString(",")}")

}