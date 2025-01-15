package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.BLEController
import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.device.BLEDevice
import com.supermegazinc.ble.scanner.BLEScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BLEControllerTestImpl(
    private val name: String,
    private val testSuite: BLETestSuite,
    private val coroutineScope: CoroutineScope
): BLEController {

    private val _adapter = BLEAdapterTestImpl(name, testSuite, coroutineScope)
    override val adapter: BLEAdapter
        get() = _adapter

    private val _device = MutableStateFlow<BLEDevice?>(null)
    override val device: StateFlow<BLEDevice?>
        get() = _device.asStateFlow()

    override val scanner: BLEScanner by lazy {
        BLEScannerTestImpl()
    }

    override fun clearDevice() {
        _device.update { null }
    }

    override fun setDevice(mac: String, mtu: Int): BLEDevice {
        val device = BLEDeviceTestImpl(
            mac,
            name,
            adapter,
            testSuite,
            coroutineScope
        )
        _device.update { device }
        return device
    }

}