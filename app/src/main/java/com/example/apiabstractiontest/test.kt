package com.example.apiabstractiontest

import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

class LoggerCustom2: Logger {
    override fun d(tag: String?, message: String) {
        println("[D] - $tag - $message")
    }

    override fun e(tag: String?, message: String) {
        println("[E] - $tag - $message")
    }

    override fun i(tag: String?, message: String) {
        println("[I] - $tag - $message")
    }
}

fun main() {

    //val logger = LoggerCustom2()
    //val bleTestSuite = BLETestSuite(
    //    logger,
    //    CoroutineScope(Dispatchers.Default)
    //)
    //val bleDevice: com.supermegazinc.ble.device.BLEDevice = BLEDeviceTestImpl(
    //    mac = "abc123",
    //    name = "ble",
    //    mtu = 516,
    //    logger = logger,
    //    bleTestSuite = bleTestSuite,
    //    coroutineContext = CoroutineScope(Dispatchers.Default).coroutineContext
    //)
//
    //runBlocking {
    //    launch {
    //        launch {
    //            bleDevice.characteristics.messageTest(TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID).collect { message->
    //                println(message)
    //            }
    //        }
    //        launch {
    //            bleDevice.characteristics.collect {
    //                println("Caracteristica: " + it)
    //            }
    //        }
    //        launch {
    //            bleDevice.connect()
    //        }
    //    }.join()
    //}

    val set1 = setOf(1,5,7,2)
    val set2 = setOf(2,5,7,1)
    println(set1==set2)
}