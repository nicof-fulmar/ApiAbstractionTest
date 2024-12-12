package com.supermegazinc.ble_upgrade.model

import com.supermegazinc.ble.scanner.model.BLEScannedDevice

sealed interface BLEUpgradeConnectionStatus {
    data class Disconnected(val reason: BLEUpgradeDisconnectReason): BLEUpgradeConnectionStatus
    data class Connecting(val device: BLEScannedDevice): BLEUpgradeConnectionStatus
    data class Connected(val device: BLEScannedDevice): BLEUpgradeConnectionStatus
    data class Reconnecting(val device: BLEScannedDevice, val from: Long): BLEUpgradeConnectionStatus
}