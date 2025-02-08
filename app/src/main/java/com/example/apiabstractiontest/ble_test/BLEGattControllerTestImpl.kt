package com.example.apiabstractiontest.ble_test

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import com.supermegazinc.ble.gatt.BLEGattController
import com.supermegazinc.ble.gatt.BLEGattControllerImpl
import com.supermegazinc.ble.gatt.BLEGattControllerImpl.Companion
import com.supermegazinc.ble.gatt.characteristic.BLEGattCharacteristic
import com.supermegazinc.ble.gatt.model.BLEMessageEvent
import com.supermegazinc.ble.gatt.model.BLESessionConnectionEvent
import com.supermegazinc.ble.gatt.model.BLESessionServiceEvent
import com.supermegazinc.ble.gatt.service.BLEGattService
import com.supermegazinc.escentials.waitForNextWithTimeout
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@SuppressLint("MissingPermission")
class BLEGattControllerTestImpl(
    private val bleTestSuite: BLETestSuite,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) : BLEGattController {

    companion object {
        const val LOG_KEY = "BLE-GATT"
    }

    private val _services = MutableStateFlow<List<BLEGattService>>(emptyList())
    override val services: StateFlow<List<BLEGattService>>
        get() = _services.asStateFlow()

    private val _characteristics = MutableStateFlow<List<BLEGattCharacteristic>>(emptyList())
    override val characteristics: StateFlow<List<BLEGattCharacteristic>>
        get() = _characteristics.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<BLESessionConnectionEvent>()
    override val connectionEvents: SharedFlow<BLESessionConnectionEvent>
        get() = _connectionEvents.asSharedFlow()

    override val instance: StateFlow<BluetoothGatt?> = MutableStateFlow<BluetoothGatt?>(null).asStateFlow()

    private val _messageEvents = MutableSharedFlow<BLEMessageEvent>(extraBufferCapacity = 1000)
    override val messageEvents: SharedFlow<BLEMessageEvent>
        get() = _messageEvents.asSharedFlow()

    private val _serviceEvents = MutableSharedFlow<BLESessionServiceEvent>()
    override val serviceEvents: SharedFlow<BLESessionServiceEvent>
        get() = _serviceEvents.asSharedFlow()

    private fun <T> SendChannel<T>.trySendWrapper(data: T): ChannelResult<Unit> {
        val sendResult = trySend(data)
        if(sendResult.isFailure) {
            logger.e(LOG_KEY, "[CRITIC] - No se pudo agregar el mensaje al canal")
        }
        return sendResult
    }

    private fun clearServicesAndCharacteristics() {
        logger.d(LOG_KEY, "Limpiando servicios y caracteristicas")
        _characteristics.update { previousCharacteristics ->
            previousCharacteristics.forEach { it.close() }
            emptyList()
        }
        _services.update { emptyList() }
    }

    private fun updateServices(newServices: List<UUID>) {
        _services.update { currentServices->
            val newServicesUUIDs = newServices.toSet()
            val currentServicesUUIDs = currentServices.map { it.uuid }.toSet()
            if(newServicesUUIDs == currentServicesUUIDs) return@update currentServices

            logger.d(LOG_KEY, "Servicios actualizados: ${newServicesUUIDs.toList()}")

            return@update newServices.map { BLEGattServiceTestImpl(it) }
        }
    }

    private fun updateCharacteristics(newCharacteristics: List<UUID>) {
        _characteristics.update { currentCharacteristics->
            val newCharacteristicsUUIDs = newCharacteristics.toSet()
            val currentCharacteristicsUUIDs = currentCharacteristics.map { it.uuid }.toSet()
            if(newCharacteristicsUUIDs == currentCharacteristicsUUIDs) return@update currentCharacteristics

            val noLongerExist = currentCharacteristics.filter { it.uuid !in newCharacteristicsUUIDs }
            noLongerExist.forEach { it.close() }
            val keepUntouched = currentCharacteristics.filter { it.uuid in newCharacteristicsUUIDs }
            val newOnes = newCharacteristics
                .filter { it !in currentCharacteristicsUUIDs }
                .map {
                    BLEGattCharacteristicTestImpl(
                        bleTestSuite,
                        it,
                        this,
                        logger,
                        coroutineScope
                    )
                }
            return@update (keepUntouched + newOnes).also { result->
                logger.d(LOG_KEY, "Caracteristicas actualizadas: ${result.joinToString { it.uuid.toString() + "[${System.identityHashCode(it)}]" }}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun readCharacteristic(uuid: UUID) {
        logger.d(LOG_KEY, "Forzando lectura caracteristica: $uuid")
        val characteristic = _characteristics.value.firstOrNull { it.uuid == uuid } ?: return
        bleTestSuite.forceRead(characteristic.uuid)
    }

    override fun discoverServices() {
        logger.d(LOG_KEY, "Descubriendo servicios")
        bleTestSuite.onDiscoverServices()
        coroutineScope.launch {
            _serviceEvents.emit(BLESessionServiceEvent.SUCCESS)
        }
    }

    override fun requestMtu(mtu: Int) {
        logger.d(LOG_KEY, "Solicitando mtu: $mtu")
    }

    private var sessionJob: Job? = null
    private fun runSession() {
        sessionJob?.cancel()
        sessionJob = coroutineScope.launch {
            delay(2000)
            bleTestSuite.onConnectGatt()
            _connectionEvents.emit(BLESessionConnectionEvent.CONNECTED)
            launch {
                bleTestSuite.receiveMessages.collect { msgEvent->
                    _messageEvents.emit(msgEvent)
                }
            }
            launch {
                bleTestSuite.characteristics.collect {
                    updateCharacteristics(it)
                }
            }
            launch {
                bleTestSuite.services.collect {
                    updateServices(it)
                }
            }
            launch {
                bleTestSuite.lostConnectionTrigger.collect {
                    _connectionEvents.emit(BLESessionConnectionEvent.CONNECTION_LOST)
                }
            }
        }
    }

    private var connectJob: Job? = null
    @SuppressLint("MissingPermission")
    override suspend fun startSession(): Boolean {
        connectJob?.cancel()
        connectJob = Job()
        return try {
            withContext(coroutineScope.coroutineContext + connectJob!!) {
                logger.d(LOG_KEY, "Iniciando sesion..")

                runSession()
                val connectionResult = try {
                    _connectionEvents.waitForNextWithTimeout(10000)
                } catch (e: CancellationException) {
                    logger.e(LOG_KEY, "No se pudo iniciar la sesion: Timeout")
                    return@withContext false
                }

                if (connectionResult == BLESessionConnectionEvent.CONNECTED) {
                    logger.d(LOG_KEY, "Sesion iniciada correctamente")
                    return@withContext true
                } else {
                    logger.e(LOG_KEY, "No se pudo iniciar la sesion")
                    return@withContext false
                }
            }
        } catch (e: CancellationException) {
            logger.e(LOG_KEY, "No se pudo iniciar la sesion: Cancelado")
            false
        }
    }

    override fun endSession() {
        logger.d(BLEGattControllerImpl.LOG_KEY, "Terminando sesion")
        connectJob?.cancel()
        clearServicesAndCharacteristics()
        sessionJob?.cancel()
    }

    override fun close() {
        logger.d(LOG_KEY, "Cerrando GATT")
        endSession()
    }

}