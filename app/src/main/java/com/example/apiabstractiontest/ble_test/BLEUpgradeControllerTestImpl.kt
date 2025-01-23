package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.BLEController
import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.escentials.calculateTimeMs
import com.supermegazinc.escentials.waitForNextWithTimeout
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class BLEUpgradeControllerTestImpl(
    private val bleController: BLEController,
    private val logger: Logger,
    coroutineContext: CoroutineContext
) : BLEUpgradeController {

    private val bleUpgradeScope = CoroutineScope(coroutineContext)

    private companion object {
        private const val LOG_KEY = "BLE-UPGRADE"
    }

    override val adapter: BLEAdapter
        get() = bleController.adapter

    private val _status = MutableStateFlow(BLEUpgradeConnectionStatus.Disconnected)
    override val status: StateFlow<BLEUpgradeConnectionStatus>
        get() = _status.asStateFlow()

    private fun updateStatus(newStatus: BLEUpgradeConnectionStatus) {
        logger.i(LOG_KEY, "ACTUALIZANDO ESTADO: $newStatus")
        _status.update { newStatus }
    }

    init {
        bleUpgradeScope.launch {
            _status.collect { newStatus->
                logger.i(LOG_KEY, "ESTADO ACTUALIZADO: $newStatus")
            }
        }
        bleUpgradeScope.coroutineContext[Job]?.invokeOnCompletion {
            println()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val characteristics: StateFlow<List<BLEDeviceCharacteristic>> by lazy {
        bleController.device.flatMapLatest { device ->
            device?.characteristics ?: flowOf(emptyList())
        }.stateIn(
            scope = bleUpgradeScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    }

    override suspend fun connect(name: String, timeoutMillis: Long, servicesUUID: List<UUID>, mtu: Int): Boolean {
        return withContext(bleUpgradeScope.coroutineContext) {
            logger.i(LOG_KEY, "CON - $name - Inicio")
            updateStatus(BLEUpgradeConnectionStatus.Connecting)

            run {
                clear()
                logger.d(LOG_KEY, "CON - $name - Comenzando busqueda (${timeoutMillis}ms)..")

                val startMs = System.currentTimeMillis()

                bleController.scanner.start(servicesUUID)

                val scannedDevice = try {
                    bleController.scanner.scannedDevices
                        .map { tList->
                            tList.firstOrNull { tDevice ->
                                name == tDevice.name
                            }
                        }
                        .filterNotNull()
                        .waitForNextWithTimeout(timeoutMillis)
                } catch (_: TimeoutCancellationException) {
                    logger.e(LOG_KEY, "CON - $name - No se encontró el dispositivo")
                    return@run false
                }

                logger.d(LOG_KEY, "CON - $name - Dispositivo encontrado! mac: ${scannedDevice.mac} (${calculateTimeMs(startMs)}ms)")
                logger.d(LOG_KEY, "CON - $name - Deteniendo busqueda..")

                bleController.scanner.stop()
                bleController.scanner.clear()

                logger.d(LOG_KEY, "CON - $name - Seteando..")

                val bleDevice = bleController.setDevice(scannedDevice.mac, mtu)
                if(bleDevice==null) {
                    logger.e(LOG_KEY, "CON - $name - No se pudo setear")
                    return@run false
                }

                logger.d(LOG_KEY, "CON - $name - Conectando dispositivo..")

                if(!bleDevice.connect()) {
                    logger.e(LOG_KEY, "CON - $name - No se pudo conectar")
                    return@run false
                } else {
                    logger.i(LOG_KEY, "CON - $name - Conexion exitosa!")
                    observeConnection()
                    return@run true
                }
            }.let { result->
                if(!result) {
                    clear()
                    updateStatus(BLEUpgradeConnectionStatus.Disconnected)
                } else {
                    updateStatus(BLEUpgradeConnectionStatus.Connected)
                }
                result
            }
        }
    }

    override fun discoverServices() {
        logger.i(LOG_KEY, "Descubriendo servicios")
        bleController.device.value?.discoverServices()
    }

    private var observeConnectionJob: Job? = null
    private fun observeConnection() {
        observeConnectionJob?.cancel()
        reconnectJob?.cancel()
        observeConnectionJob = bleUpgradeScope.launch {
            logger.d(LOG_KEY, "OBS - Observando conexion")
            bleController.device.collectLatest { tDevice->
                coroutineScope {
                    tDevice
                        ?.status
                        ?.filterIsInstance<BLEDeviceStatus.Disconnected>()
                        ?.filter { it.reason != BLEDisconnectionReason.DISCONNECTED }
                        ?.collectLatest {_->
                            logger.e(LOG_KEY, "OBS - Desconexion detectada! Intentando reconectar..")
                            reconnect()
                        }
                }
            }
        }
    }

    private var reconnectJob: Job? = null
    private fun reconnect() {
        reconnectJob?.cancel()
        reconnectJob = bleUpgradeScope.launch {
            updateStatus(BLEUpgradeConnectionStatus.Reconnecting)
            while(isActive) {
                delay(1000)
                logger.d(LOG_KEY, "OBS - Intento de reconexion..")
                if(bleController.device.value?.connect()==true) {
                    logger.i(LOG_KEY, "OBS - Reconectado!")
                    updateStatus(BLEUpgradeConnectionStatus.Connected)
                    break
                }
            }
        }
    }

    override fun disconnect() {
        logger.i(LOG_KEY, "DES - Desconectando..")
        clear()
        observeConnectionJob?.cancel()
        reconnectJob?.cancel()
        updateStatus(BLEUpgradeConnectionStatus.Disconnected)
        logger.i(LOG_KEY, "DES - Desconectado!")
    }

    private fun clear() {
        observeConnectionJob?.cancel()
        reconnectJob?.cancel()
        bleController.scanner.stop()
        bleController.scanner.clear()
        bleController.clearDevice()
    }

}