package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.scanner.BLEScanner
import com.supermegazinc.ble.scanner.model.BLEScannedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class BLEScannerTestImpl: BLEScanner {

    private val _scannedDevices = MutableStateFlow<List<BLEScannedDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BLEScannedDevice>>
        get() = _scannedDevices.asStateFlow()

    override fun clear() {
        _scannedDevices.update { emptyList() }
    }

    override fun start(serviceUUID: List<UUID>?) {
        _scannedDevices.update {
            listOf(
                BLEScannedDevice(
                    "TAXI-PRUEBA1-",
                    "ABC123"
                )
            )
        }
    }

    override fun stop() {

    }
}