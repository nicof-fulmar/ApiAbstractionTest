package com.example.apiabstractiontest.ble_test

import android.util.Log
import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.adapter.model.BLEAdapterState
import com.supermegazinc.ble.device.BLEDevice
import com.supermegazinc.ble.device.BLEDeviceImpl.Companion.LOG_KEY
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.device.service.BLEDeviceService
import com.supermegazinc.ble.device.service.BLEDeviceServiceImpl
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import com.supermegazinc.ble.gatt.model.BLEGattConnectError
import com.supermegazinc.escentials.Result
import com.supermegazinc.escentials.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class BLEDeviceTestImpl(
    private val _mac: String,
    private val _name: String,
    private val adapter: BLEAdapter,
    private val coroutineScope: CoroutineScope
): BLEDevice {

    private val _characteristics = MutableStateFlow<List<BLEDeviceCharacteristic>>(emptyList())
    override val characteristics: StateFlow<List<BLEDeviceCharacteristic>>
        get() = _characteristics

    override val mac: String
        get() = _mac

    override val name: String
        get() = _name

    private val _services = MutableStateFlow<List<BLEDeviceService>>(emptyList())
    override val services: StateFlow<List<BLEDeviceService>>
        get() = _services.asStateFlow()

    private val _status = MutableStateFlow<BLEDeviceStatus>(BLEDeviceStatus.Disconnected(BLEDisconnectionReason.DISCONNECTED))
    override val status: StateFlow<BLEDeviceStatus>
        get() = _status.asStateFlow()

    override fun close() {

    }

    private fun clear() {
        _characteristics.update { emptyList() }
        _services.update { emptyList() }
    }

    private var connectScope: CoroutineScope? = null
    override suspend fun connect(): Result<Unit, BLEGattConnectError> {
        connectScope?.cancel()
        connectScope = CoroutineScope(coroutineScope.coroutineContext)
        return withContext(connectScope!!.coroutineContext) {
            try {
                val adapterState = adapter.state.value
                if(adapterState !is Status.Ready || adapterState.data != BLEAdapterState.ON) return@withContext Result.Fail<Unit, BLEGattConnectError>(BLEGattConnectError.CANT_CONNECT)

                delay(5000)
                _services.update {
                    listOf(
                        BLEDeviceServiceTestImpl(UUIDs.SERVICE_MAIN_UUID)
                    )
                }
                _characteristics.update {
                    listOf(
                        BLEDeviceCharacteristicTestImpl(
                            UUIDs.CHARACTERISTIC_RECEIVE_KEY_UUID,
                            UUIDs.TANGO_PUBLIC_KEY,
                            coroutineScope
                        ),
                        BLEDeviceCharacteristicTestImpl(
                            UUIDs.CHARACTERISTIC_SEND_KEY_UUID,
                            null,
                            coroutineScope
                        ),
                        BLEDeviceCharacteristicTestImpl(
                            UUIDs.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID,
                            null,
                            coroutineScope
                        ),
                    )
                }
                return@withContext Result.Success<Unit, BLEGattConnectError>(Unit)

            } catch (e: CancellationException) {
                Log.e(LOG_KEY, "Conexion cancelada")
                return@withContext Result.Fail<Unit, BLEGattConnectError>(BLEGattConnectError.CANCELED)
            }
        }
    }

    override suspend fun disconnect() {
        clear()
        _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.DISCONNECTED) }
    }

    private suspend fun observeAdapter() {
        adapter.state
            .filterIsInstance<Status.Ready<BLEAdapterState>>()
            .filter { it.data != BLEAdapterState.ON }
            .distinctUntilChanged()
            .collect {
                clear()
                Log.e("TEST-BLE-DEVICE", "Adaptador desconectado")
                _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.CONNECTION_LOST) }
            }
    }

    init {
        coroutineScope.launch {
            observeAdapter()
        }
    }
}