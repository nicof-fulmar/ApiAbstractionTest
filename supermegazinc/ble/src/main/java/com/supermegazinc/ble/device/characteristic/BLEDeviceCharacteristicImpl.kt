package com.supermegazinc.ble.device.characteristic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import com.supermegazinc.ble.gatt.BLEGattController
import com.supermegazinc.ble.gatt.model.BLEGattEvent
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class BLEDeviceCharacteristicImpl(
    private val bleGattController: BLEGattController,
    private val characteristic: BluetoothGattCharacteristic,
    private val logger: Logger,
    coroutineScope: CoroutineScope
) : BLEDeviceCharacteristic {

    companion object {
        const val LOG_KEY = "BLE-CHARACTERISTIC"
    }

    override val uuid: UUID
        get() = characteristic.uuid

    private val _message = MutableStateFlow<ByteArray?>(null)
    override val message: StateFlow<ByteArray?>
        get() = _message.asStateFlow()

    override fun setNotification(state: Boolean) {
        bleGattController.instance.value!!.setCharacteristicNotification(characteristic, state)
    }

    override fun forceRead() {
        bleGattController.readCharacteristic(this@BLEDeviceCharacteristicImpl.uuid)
    }

    @SuppressLint("MissingPermission")
    override suspend fun send(message: ByteArray) {
        logger.i(LOG_KEY, "$uuid: Sending message")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bleGattController.instance.value?.writeCharacteristic(characteristic, message, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            characteristic.setValue(message)
            bleGattController.instance.value?.writeCharacteristic(characteristic)
        }
    }

    private var job: Job? = null
    init {
        job = coroutineScope.launch {
            bleGattController.events
                .filterIsInstance<BLEGattEvent.Message>()
                .filter { it.characteristic == characteristic }
                .collect { tMessage->
                    logger.i(LOG_KEY, "$uuid: Mensaje recibido: '${tMessage.message.toList().joinToString(",")}'")
                    _message.update { tMessage.message }
                }
        }
    }

    override fun close() {
        job?.cancel()
        bleGattController.instance.value?.setCharacteristicNotification(characteristic,false)
    }

}