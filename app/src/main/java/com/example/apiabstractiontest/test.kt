package com.example.apiabstractiontest

import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

fun main() {
    /*
        BLEUpgradeController()
    runBlocking { while(true) {} }
     */

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

    runBlocking { while(true) {} }
}