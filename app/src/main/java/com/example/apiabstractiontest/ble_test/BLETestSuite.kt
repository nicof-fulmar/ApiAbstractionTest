package com.example.apiabstractiontest.ble_test

import com.fulmar.tango.layer1.config.TangoL1Config
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.service.BLEDeviceService
import com.supermegazinc.ble.gatt.model.BLEMessageEvent
import com.supermegazinc.ble.gatt.model.BLESessionConnectionEvent
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val hiddenServices = MutableStateFlow<List<BLEDeviceService>>(emptyList())
    val services = MutableStateFlow<List<BLEDeviceService>>(emptyList())

    fun onConnectGatt() {
        characteristics.update {
            listOf(
                BLEDeviceCharacteristicTestImpl(TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID),
                BLEDeviceCharacteristicTestImpl(TangoL1Config.CHARACTERISTIC_RECEIVE_KEY_UUID)
            )
        }
        hiddenServices.update {
            listOf(
                BLEDeviceServiceTestImpl(TangoL1Config.SERVICE_MAIN_UUID)
            )
        }
    }

    fun onDiscoverServices() {
        services.update { hiddenServices.value }
    }

}