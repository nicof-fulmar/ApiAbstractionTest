package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.adapter.model.BLEAdapterState
import com.supermegazinc.ble.device.BLEDevice
import com.supermegazinc.ble.device.BLEDeviceImpl
import com.supermegazinc.ble.device.BLEDeviceImpl.Companion
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.device.service.BLEDeviceService
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import com.supermegazinc.ble.mappers.toBLEDeviceCharacteristic
import com.supermegazinc.escentials.Status
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class BLEDeviceTestImpl(
    override val mac: String,
    override val name: String?,
    override val mtu: Int,
    private val logger: Logger,
    private val bleTestSuite: BLETestSuite,
    coroutineContext: CoroutineContext,
): BLEDevice {

    companion object {
        const val LOG_KEY = "BLE-DEVICE"
    }

    private val deviceJob = Job()
    private val deviceCoroutineScope = CoroutineScope(coroutineContext + deviceJob)

    private val _services = MutableStateFlow<List<BLEDeviceService>>(emptyList())
    override val services: StateFlow<List<BLEDeviceService>>
        get() = _services.asStateFlow()

    private val _characteristics = MutableStateFlow<List<BLEDeviceCharacteristic>>(emptyList())
    override val characteristics: StateFlow<List<BLEDeviceCharacteristic>>
        get() = _characteristics.asStateFlow()

    private val _status = MutableStateFlow<BLEDeviceStatus>(BLEDeviceStatus.Disconnected(BLEDisconnectionReason.DISCONNECTED))
    override val status: StateFlow<BLEDeviceStatus>
        get() = _status.asStateFlow()

    override fun close() {
        logger.d(LOG_KEY, "Cerrando dispositivo..")
        connectJob?.cancel()
        observeJob.cancel()
        clearServicesAndCharacteristics()
        deviceCoroutineScope.cancel()
        _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.DISCONNECTED) }
        logger.d(LOG_KEY, "Dispositivo cerrado..")
    }
    private fun clearServicesAndCharacteristics() {
        logger.d(LOG_KEY, "Limpiando servicios y caracteristicas")
        _characteristics.update { previousCharacteristics ->
            previousCharacteristics.forEach { it.close() }
            emptyList()
        }
        _services.update { emptyList() }
    }

    private var connectJob: Job? = null
    override suspend fun connect(): Boolean {
        connectJob?.cancel()
        connectJob = Job()

        return try {
            withContext(deviceCoroutineScope.coroutineContext + connectJob!!) {
                run {
                    logger.d(BLEDeviceImpl.LOG_KEY, "Conectando dispositivo..")

                    clearServicesAndCharacteristics()
                    _status.update { BLEDeviceStatus.Connecting }

                    delay(3000)

                    return@run true
                }.let { result->
                    if(!result) {
                        logger.e(BLEDeviceImpl.LOG_KEY, "No se pudo conectar el dispositivo")
                        _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.CANT_CONNECT) }
                        clearServicesAndCharacteristics()
                    } else {
                        delay(500)
                        bleTestSuite.onConnectGatt()
                        discoverServices()
                        delay(500)
                        logger.d(BLEDeviceImpl.LOG_KEY, "Dispositivo conectado")
                        _status.update { BLEDeviceStatus.Connected }
                    }
                    result
                }
            }
        } catch (e: CancellationException) {
            logger.e(BLEDeviceImpl.LOG_KEY, "Conexion cancelada")
            false
        }

    }

    override suspend fun disconnect() {
        logger.d(LOG_KEY, "Desconectando dispositivo..")
        connectJob?.cancel()
        clearServicesAndCharacteristics()
        _status.update { BLEDeviceStatus.Disconnected(BLEDisconnectionReason.DISCONNECTED) }
        logger.d(LOG_KEY, "Dispositivo desconectado")
    }

    override fun discoverServices() {
        logger.d(LOG_KEY, "Descubriendo servicios")
        bleTestSuite.onDiscoverServices()
    }

    private suspend fun observeCharacteristics() {
        bleTestSuite.characteristics
            .collect { mappedCharacteristics->
                logger.d(LOG_KEY, "Caracteristicas actualizadas")
                _characteristics.update { previousCharacteristics ->
                    val mappedUuidSet = mappedCharacteristics.map { it.uuid }.toSet()
                    val previousUuidSet = previousCharacteristics.map { it.uuid }.toSet()

                    val noLongerExist = previousCharacteristics.filter { it.uuid !in mappedUuidSet }
                    noLongerExist.forEach { it.close() }
                    val keepUntouched = previousCharacteristics.filter { it.uuid in mappedUuidSet }
                    val newCharacteristics = mappedCharacteristics.filter { it.uuid !in previousUuidSet }
                    keepUntouched + newCharacteristics
                }
            }
    }

    private val observeJob: Job
    init {
        observeJob = deviceCoroutineScope.launch {
            launch { observeCharacteristics() }
        }
    }
}