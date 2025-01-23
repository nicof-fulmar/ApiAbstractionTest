package com.example.apiabstractiontest

import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.coroutineContext

class BLEDevice() {

    private val _status = MutableStateFlow<BLEDeviceStatus>(BLEDeviceStatus.Disconnected(BLEDisconnectionReason.DISCONNECTED))
    val status: StateFlow<BLEDeviceStatus>
        get() = _status.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000)
            _status.update { BLEDeviceStatus.Connected }
            delay(5000)
            _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.CONNECTION_LOST) }
        }
    }
}

class BLEController() {

    private val _device = MutableStateFlow<BLEDevice?>(null)
    val device: StateFlow<BLEDevice?>
        get() = _device.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000)
            _device.update { BLEDevice() }
        }
    }
}

class BLEUpgradeController() {

    private val bleController = BLEController()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            bleController.device.collectLatest { tDevice->
                coroutineScope {
                    tDevice
                        ?.status
                        ?.filterIsInstance<BLEDeviceStatus.Disconnected>()
                        ?.filter { it.reason != BLEDisconnectionReason.DISCONNECTED }
                        ?.collectLatest {_->
                            print("OBS - Desconexion detectada! Intentando reconectar..")
                        }
                }
            }
        }
    }
}

val flow1 = flow<String> {
    var str = "+"
    while(coroutineContext.isActive) {
        emit(str)
        str+="+"
        delay(5000)
    }
}

val flow2 = flow<String> {
    var str = "1"
    while(coroutineContext.isActive) {
        emit(str)
        str+="1"
        delay(1000)
    }
}

suspend fun doSomething() {
    flow1.collectLatest {
        flow2.collectLatest { one ->
            println(one)
        }
    }
}

var doSomething2Job: Job? = null
suspend fun doSomething2() {
    doSomething2Job?.cancel()
    doSomething2Job = CoroutineScope(Dispatchers.IO).launch {
        launch { doSomething() }
        launch { doSomething() }
        launch { doSomething() }
    }
}


fun main() {
    /*

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    coroutineScope.coroutineContext[Job]?.invokeOnCompletion {
        println("Scope " + it)
    }

    val task1Job = Job()
    task1Job.invokeOnCompletion {
        println("Job: " + it)
    }

    val task1CoroutineScope = CoroutineScope(coroutineScope.coroutineContext + task1Job)
    task1CoroutineScope.cancel()

    task1CoroutineScope.coroutineContext[Job]?.invokeOnCompletion {
        println("TaskCoroutine: " + it)
    }

     */

    val firmware = Channel<ByteArray>(capacity = 100, onBufferOverflow = BufferOverflow.SUSPEND)

    runBlocking {
        launch {
            firmware.consumeAsFlow().collect { message->
                delay(1000)
                println(message.decodeToString())
            }
            println("done")
        }
        launch {
            firmware.send("hola".toByteArray())
            firmware.send("como".toByteArray())
            firmware.send("estas".toByteArray())
            delay(1000)
            firmware.send("aaa".toByteArray())
        }
    }
}