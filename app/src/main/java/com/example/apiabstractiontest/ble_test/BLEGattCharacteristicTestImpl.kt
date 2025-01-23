package com.example.apiabstractiontest.ble_test

import android.annotation.SuppressLint
import com.supermegazinc.ble.gatt.BLEGattController
import com.supermegazinc.ble.gatt.characteristic.BLEGattCharacteristic
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class BLEGattCharacteristicTestImpl(
    private val bleTestSuite: BLETestSuite,
    override val uuid: UUID,
    private val bleGattController: BLEGattController,
    private val logger: Logger,
    coroutineScope: CoroutineScope
) : BLEGattCharacteristic {

    companion object {
        const val LOG_KEY = "BLE-CHARACTERISTIC"
    }

    private val _message = Channel<ByteArray?>(capacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val message: ReceiveChannel<ByteArray?>
        get() = _message

    override fun setNotification(state: Boolean) {
        logger.d(LOG_KEY, "$uuid: Setear notification: $state")
        bleTestSuite.setNotification(uuid, state)
    }

    override fun forceRead() {
        logger.d(LOG_KEY, "$uuid: Forzar lectura")
        bleGattController.readCharacteristic(this.uuid)
    }

    @SuppressLint("MissingPermission")
    override suspend fun send(message: ByteArray) {
        val messageList = message.toList()
        logger.d(
            LOG_KEY,
            "$uuid: Enviando mensaje[BYT]: [${messageList.size}][${messageList.joinToString(",")}]"
        )
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        //    bleGattController.instance.value?.writeCharacteristic(characteristic, message, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        //} else {
        //    characteristic.setValue(message)
        //    bleGattController.instance.value?.writeCharacteristic(characteristic)
        //}
    }

    private var receiveMessagesJob: Job = coroutineScope.launch {
        bleGattController.messageEvents
            .filter { it.characteristicUUID == uuid }
            .collect { tMessage->
                val messageList = tMessage.message.toList()
                logger.d(LOG_KEY,"$uuid: Mensaje recibido[BYT]: [${messageList.size}][${messageList.joinToString(",")}]")
                _message.send(tMessage.message)
            }
    }

    override fun close() {
        logger.d(LOG_KEY, "$uuid: Cerrando")
        receiveMessagesJob.cancel()
        _message.close()
        //bleGattController.instance.value?.setCharacteristicNotification(characteristic,false)
    }

}