package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class BLEDeviceCharacteristicTestImpl(
    private val _uuid: UUID,
    private val value: ByteArray? = null,
    private val coroutineScope: CoroutineScope,
): BLEDeviceCharacteristic {

    private val _message = MutableStateFlow<ByteArray?>(null)
    override val message: StateFlow<ByteArray?>
        get() = _message.asStateFlow()

    override val uuid: UUID
        get() = _uuid

    override fun close() {
        _message.update { null }
    }

    override fun forceRead() {
        _message.update { value }
    }

    override suspend fun send(message: ByteArray) {

    }

    private var notificationJob: Job? = null
    override fun setNotification(state: Boolean) {
        notificationJob?.cancel()
        if(!state) return
        coroutineScope.launch {
            while(isActive) {
                _message.update { value }
                delay(2000)
            }
        }
    }
}