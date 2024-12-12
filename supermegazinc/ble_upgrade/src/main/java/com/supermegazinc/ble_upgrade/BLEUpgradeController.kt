package com.supermegazinc.ble_upgrade

import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionError
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.escentials.Result
import kotlinx.coroutines.flow.StateFlow

interface BLEUpgradeController {

    val adapter: BLEAdapter
    val status: StateFlow<BLEUpgradeConnectionStatus>
    val characteristics: StateFlow<List<BLEDeviceCharacteristic>>
    suspend fun connect(
        name: String,
        timeoutMillis: Long
    ): Result<Unit, BLEUpgradeConnectionError>
    fun disconnect()

}