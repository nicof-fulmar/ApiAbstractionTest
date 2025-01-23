package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(FlowPreview::class)
fun Flow<List<BLEDeviceCharacteristic>>.messageTest(uuid: UUID): Flow<ByteArray> {
    return mapNotNull { characteristics->
        characteristics.firstOrNull {
            it.uuid == uuid
        }
    }
        .distinctUntilChanged { old, new->
            old === new
        }
        .map { characteristic ->
            characteristic.setNotification(true)
            characteristic
                .message
                .consumeAsFlow()
                .filterNotNull().also { flow->
                    CoroutineScope(Dispatchers.IO).launch {
                        flow.collectLatest {
                            println("MENSAJE: " + it.toList())
                        }
                    }
                }
        }
        .flattenConcat()
}