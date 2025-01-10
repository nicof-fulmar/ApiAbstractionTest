package com.supermegazinc.ble_upgrade

import com.supermegazinc.ble.BLEController
import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import com.supermegazinc.ble.scanner.model.BLEScannedDevice
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionError
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.ble_upgrade.model.BLEUpgradeDisconnectReason
import com.supermegazinc.escentials.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class BLEUpgradeControllerTestImpl(
    private val bleController: BLEController
) : BLEUpgradeController {

    override val adapter: BLEAdapter
        get() = bleController.adapter

    private val _status = MutableStateFlow<BLEUpgradeConnectionStatus>(
        BLEUpgradeConnectionStatus.Disconnected(
            BLEUpgradeDisconnectReason.BLE(BLEDisconnectionReason.DISCONNECTED)
        )
    )
    override val status: StateFlow<BLEUpgradeConnectionStatus>
        get() = _status.asStateFlow()

    private val _characteristics = MutableStateFlow<List<BLEDeviceCharacteristic>>(emptyList())
    override val characteristics: StateFlow<List<BLEDeviceCharacteristic>>
        get() = _characteristics

    override suspend fun connect(
        name: String,
        timeoutMillis: Long,
        servicesUUID: List<UUID>,
        mtu: Int
    ): Result<Unit, BLEUpgradeConnectionError> {
        _status.update { BLEUpgradeConnectionStatus.Connected(BLEScannedDevice(name, "")) }
        return Result.Success(Unit)
    }

    override fun disconnect() {
        _status.update { BLEUpgradeConnectionStatus.Disconnected(
            BLEUpgradeDisconnectReason.BLE(BLEDisconnectionReason.DISCONNECTED)
        ) }
    }
}