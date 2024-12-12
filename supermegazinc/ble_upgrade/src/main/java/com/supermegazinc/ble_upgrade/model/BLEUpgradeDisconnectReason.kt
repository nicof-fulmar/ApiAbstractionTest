package com.supermegazinc.ble_upgrade.model

import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason

interface BLEUpgradeDisconnectReason {
    data class BLE(val error: BLEDisconnectionReason): BLEUpgradeDisconnectReason
    data class Upgrade(val error: BLEUpgradeConnectionError): BLEUpgradeDisconnectReason
}