package com.example.apiabstractiontest.ble_test

import android.util.Log
import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.adapter.model.BLEAdapterState
import com.supermegazinc.ble.device.BLEDevice
import com.supermegazinc.ble.device.BLEDeviceImpl.Companion.LOG_KEY
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.device.service.BLEDeviceService
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import com.supermegazinc.ble.gatt.model.BLEGattConnectError
import com.supermegazinc.escentials.Result
import com.supermegazinc.escentials.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class BLEDeviceTestImpl(
    private val _mac: String,
    private val _name: String,
    private val adapter: BLEAdapter,
    private val testSuite: BLETestSuite,
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
        connectJob?.cancel()
        _characteristics.update { emptyList() }
        _services.update { emptyList() }
    }

    private var connectJob: Job? = null
    override suspend fun connect(): Result<Unit, BLEGattConnectError> {
        connectJob?.cancel()
        connectJob = Job(coroutineScope.coroutineContext.job)

        return try {

            withContext(coroutineScope.coroutineContext + connectJob!!) {

                val adapterState = adapter.state.value
                if (adapterState !is Status.Ready || adapterState.data != BLEAdapterState.ON) {
                    return@withContext Result.Fail<Unit, BLEGattConnectError>(
                        BLEGattConnectError.CANT_CONNECT
                    )
                }

                delay(5000)

                _services.update {
                    listOf(
                        BLEDeviceServiceTestImpl(BLETestK.SERVICE_MAIN_UUID)
                    )
                }

                _characteristics.update {
                    listOf(
                        BLEDeviceCharacteristicTestImpl(
                            BLETestK.CHARACTERISTIC_RECEIVE_KEY_UUID,
                            BLETestK.TANGO_PUBLIC_KEY,
                            coroutineScope
                        ),
                        BLEDeviceCharacteristicTestImpl(
                            BLETestK.CHARACTERISTIC_SEND_KEY_UUID,
                            null,
                            coroutineScope
                        ),
                        BLEDeviceCharacteristicTestImpl(
                            BLETestK.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID,
                            null,
                            coroutineScope
                        ),
                    )
                }

                _status.update { BLEDeviceStatus.Connected }
                return@withContext Result.Success(Unit)
            }
        } catch (e: CancellationException) {
            Log.e(LOG_KEY, "Conexion cancelada")
            return Result.Fail<Unit, BLEGattConnectError>(BLEGattConnectError.CANCELED)
        }
    }

    override suspend fun disconnect() {
        clear()
        _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.DISCONNECTED) }
    }

    private suspend fun observeAdapter() {
        coroutineScope {
            _status.collectLatest { tStatus->
                if(tStatus !is BLEDeviceStatus.Disconnected) {
                    adapter.state
                        .filterIsInstance<Status.Ready<BLEAdapterState>>()
                        .filter { it.data != BLEAdapterState.ON }
                        .collect {
                            Log.e("TEST-BLE-DEVICE", "Adaptador desconectado")
                            clear()
                            _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.CONNECTION_LOST) }
                        }
                }
            }
        }
    }

    init {
        coroutineScope.launch {
            observeAdapter()
        }
        coroutineScope.launch {
            testSuite.connectionLost.collect {
                clear()
                _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.CONNECTION_LOST) }
            }
        }
    }
}