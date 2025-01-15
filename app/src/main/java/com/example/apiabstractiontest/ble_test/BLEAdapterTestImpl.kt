package com.example.apiabstractiontest.ble_test

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.adapter.model.BLEAdapterState
import com.supermegazinc.ble.device.BLEDevice
import com.supermegazinc.escentials.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BLEAdapterTestImpl(
    private val name: String,
    private val testSuite: BLETestSuite,
    private val coroutineScope: CoroutineScope
): BLEAdapter {

    private val _state = MutableStateFlow<Status<BLEAdapterState>>(Status.Ready(BLEAdapterState.ON))
    override val state: StateFlow<Status<BLEAdapterState>>
        get() = _state.asStateFlow()

    override fun bluetoothLauncher(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        TODO("Not yet implemented")
    }

    override fun getDevice(address: String, mtu: Int): BLEDevice {
        return BLEDeviceTestImpl(
            address,
            name,
            this,
            coroutineScope
        )
    }

    override fun onBluetoothLauncherResult(result: ActivityResult) {
        TODO("Not yet implemented")
    }

    init {
        coroutineScope.launch {
            testSuite.adapterOff.collectLatest {
                _state.update { Status.Ready(BLEAdapterState.OFF) }
            }
        }
    }
}