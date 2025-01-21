package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.service.BLEDeviceService
import com.supermegazinc.ble.device.service.BLEDeviceServiceImpl
import com.supermegazinc.ble.gatt.model.BLEMessageEvent
import com.supermegazinc.ble.gatt.model.BLESessionConnectionEvent
import com.supermegazinc.ble.gatt.model.BLESessionServiceEvent
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlin.math.log

class BLETestSuite(
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TEST-BLE"
    }

    val gattMessagesChannel = Channel<BLEMessageEvent>(capacity = Channel.CONFLATED)
    val gattConnectionChannel = Channel<BLESessionConnectionEvent>(capacity = Channel.CONFLATED)

    val characteristics = MutableStateFlow<List<BLEDeviceCharacteristic>>(emptyList())
    val services = MutableStateFlow<List<BLEDeviceService>>(emptyList())

    private var gattStartSessionJob: Job? = null
    fun gattStartSession() {
        gattStartSessionJob?.cancel()
        gattStartSessionJob = coroutineScope.launch {
            delay(2000)
            gattConnectionChannel.send(BLESessionConnectionEvent.CONNECTED)
            delay(3000)
            services.emit(
                listOf(
                    BLEDeviceServiceTestImpl(BLETestK.SERVICE_MAIN_UUID)
                )
            )
            characteristics.emit(
                listOf(

                )
            )
        }
    }

}