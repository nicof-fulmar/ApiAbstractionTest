package com.example.apiabstractiontest

import com.example.apiabstractiontest.ble_test.BLEDeviceCharacteristicTestImpl
import com.example.apiabstractiontest.ble_test.messageTest
import com.fulmar.tango.layer1.config.TangoL1Config
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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

    val _characteristics = MutableStateFlow<List<BLEDeviceCharacteristic>>(emptyList())
    val characteristics = _characteristics.asStateFlow()

    runBlocking {
        launch {
            launch {
                characteristics.messageTest(TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID).collect { message->
                    println(message)
                }
            }
            launch {
                characteristics.collect {
                    println("Caracteristica: " + it)
                }
            }
            launch {
                delay(1000)
                _characteristics.emit(listOf(BLEDeviceCharacteristicTestImpl(TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID)))
                delay(1000)
                _characteristics.update { it + BLEDeviceCharacteristicTestImpl(TangoL1Config.CHARACTERISTIC_RECEIVE_FIRMWARE) }
                delay(5000)
                _characteristics.emit(emptyList())
            }
        }.join()
    }
}