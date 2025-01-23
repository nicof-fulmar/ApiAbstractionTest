package com.example.apiabstractiontest.ble_test

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.adapter.model.BLEAdapterState
import com.supermegazinc.ble.device.BLEDevice
import com.supermegazinc.ble.device.BLEDeviceImpl
import com.supermegazinc.ble.gatt.BLEGattController
import com.supermegazinc.escentials.Status
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

class BLEAdapterTestImpl(
    private val gattControllerFactory: (scope: CoroutineScope) -> BLEGattController,
    private val name: String,
    private val logger: Logger,
    private val coroutineContext: CoroutineContext
): BLEAdapter {

    private val _state = MutableStateFlow<Status<BLEAdapterState>>(Status.Ready(BLEAdapterState.ON))
    override val state: StateFlow<Status<BLEAdapterState>>
        get() = _state

    override fun bluetoothLauncher(launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {

    }

    override fun getDevice(address: String, mtu: Int): BLEDevice? {
        return BLEDeviceImpl(
            name = name,
            mac = address,
            mtu = mtu,
            gattControllerFactory = {
                gattControllerFactory(it)
            },
            logger = logger,
            adapter = this,
            coroutineContext = coroutineContext
        )
    }

    override fun onBluetoothLauncherResult(result: ActivityResult) {

    }
}