package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristicImpl.Companion.LOG_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class BLEDeviceCharacteristicTestImpl(
    override val uuid: UUID
): BLEDeviceCharacteristic {

    private val _message = Channel<ByteArray?>(capacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val message: ReceiveChannel<ByteArray?>
        get() = _message

    private val receiveMessagesJob: Job
    init {
        receiveMessagesJob = CoroutineScope(Dispatchers.IO).launch {
            while(isActive) {
                _message.send("Hola".toByteArray())
                delay(1000)
            }
        }
    }

    override fun close() {
        receiveMessagesJob.cancel()
        _message.close()
    }

    override fun forceRead() {
        TODO("Not yet implemented")
    }

    override suspend fun send(message: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun setNotification(state: Boolean) {

    }
}