package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.gatt.characteristic.BLEGattCharacteristic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<List<BLEGattCharacteristic>>.messageTest(uuid: UUID): Flow<ByteArray> {
    return map { characteristics ->
        characteristics.firstOrNull { it.uuid == uuid }
    }
    .distinctUntilChanged { old, new ->
        old === new
    }
    .flatMapLatest { characteristic->
        if (characteristic != null) {
            characteristic.setNotification(true)
            characteristic.message.consumeAsFlow().filterNotNull()
        } else {
            emptyFlow()
        }
    }
}