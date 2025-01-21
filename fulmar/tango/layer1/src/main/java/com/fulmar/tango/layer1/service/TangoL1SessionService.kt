package com.fulmar.tango.layer1.service

import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import kotlinx.coroutines.flow.Flow

suspend fun tangoL1SessionService(
    connectionStatus: Flow<BLEUpgradeConnectionStatus>,
    onNewSession: () -> Unit,
    onEndSession: () -> Unit
) {
    connectionStatus.collect { status->
        if(status == BLEUpgradeConnectionStatus.Connected) onNewSession()
        else onEndSession()
    }
}