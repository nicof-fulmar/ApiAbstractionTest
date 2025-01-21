package com.example.apiabstractiontest

import com.fulmar.firmware.model.TangoFirmwareInitJson
import com.fulmar.firmware.util.dividePacket
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
import kotlin.coroutines.coroutineContext

fun main() {

    val coroutineScope = CoroutineScope(Dispatchers.IO)

    runBlocking {
        withContext(coroutineScope.coroutineContext) {
            launch {
                println(hacerAlgo())
            }
            delay(2000)
            launch {
                println(hacerAlgo())
            }
            coroutineScope.coroutineContext[Job]?.invokeOnCompletion {
                println("Completado")
            }
        }
    }
}

private var algoJob: Job? = null
suspend fun hacerAlgo(): Boolean {
    algoJob?.cancel()
    algoJob = SupervisorJob()
    return try {
        withContext(coroutineContext + algoJob!!) {
            delay(5000)
            return@withContext true
        }
    }catch (e: CancellationException) {
        println("Cancelado")
        return false
    }
}