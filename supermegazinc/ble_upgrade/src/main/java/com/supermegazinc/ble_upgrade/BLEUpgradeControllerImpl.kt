package com.supermegazinc.ble_upgrade

import com.supermegazinc.ble.BLEController
import com.supermegazinc.ble.adapter.BLEAdapter
import com.supermegazinc.ble.device.BLEDevice
import com.supermegazinc.ble.device.characteristic.BLEDeviceCharacteristic
import com.supermegazinc.ble.device.model.BLEDeviceStatus
import com.supermegazinc.ble.gatt.model.BLEDisconnectionReason
import com.supermegazinc.ble.scanner.model.BLEScannedDevice
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionError
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.ble_upgrade.model.BLEUpgradeDisconnectReason
import com.supermegazinc.escentials.Result
import com.supermegazinc.escentials.calculateTimeMs
import com.supermegazinc.escentials.waitForNextWithTimeout
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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

class BLEUpgradeControllerImpl(
    private val bleController: BLEController,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) : BLEUpgradeController {

    private companion object {
        private const val LOG_KEY = "BLE-UPGRADE"
    }

    override val adapter: BLEAdapter
        get() = bleController.adapter

    private val _status = MutableStateFlow<BLEUpgradeConnectionStatus>(
        BLEUpgradeConnectionStatus.Disconnected(
            BLEUpgradeDisconnectReason.BLE(BLEDisconnectionReason.DISCONNECTED)
        )
    )
    override val status: StateFlow<BLEUpgradeConnectionStatus>
        get() = _status.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val characteristics: StateFlow<List<BLEDeviceCharacteristic>> by lazy {
        bleController.device.flatMapLatest { device ->
            device?.characteristics ?: flowOf(emptyList())
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    }

    override suspend fun connect(name: String, timeoutMillis: Long, servicesUUID: List<UUID>, mtu: Int): Result<Unit, BLEUpgradeConnectionError> {
        return withContext(coroutineScope.coroutineContext) {
            logger.i(LOG_KEY, "CON - $name - Inicio")
            _status.update {
                BLEUpgradeConnectionStatus.Connecting(BLEScannedDevice(name, ""))
            }
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
                return@withContext cancelConnect(BLEUpgradeConnectionError.DEVICE_NOT_FOUND)
            }

            _status.update {
                BLEUpgradeConnectionStatus.Connecting(BLEScannedDevice(name, scannedDevice.mac))
            }

            logger.d(LOG_KEY, "CON - $name - Dispositivo encontrado! mac: ${scannedDevice.mac} (${calculateTimeMs(startMs)}ms)")
            logger.d(LOG_KEY, "CON - $name - Deteniendo busqueda..")

            bleController.scanner.stop()
            bleController.scanner.clear()

            logger.d(LOG_KEY, "CON - $name - Seteando..")

            val bleDevice = bleController.setDevice(scannedDevice.mac, mtu)
            if(bleDevice==null) {
                logger.e(LOG_KEY, "CON - $name - No se pudo setear")
                return@withContext cancelConnect(BLEUpgradeConnectionError.DEVICE_NOT_FOUND)
            }

            logger.d(LOG_KEY, "CON - $name - Conectando dispositivo..")

            when(bleDevice.connect()) {
                is Result.Fail -> {
                    logger.e(LOG_KEY, "CON - $name - No se pudo conectar")
                    return@withContext Result.Fail(BLEUpgradeConnectionError.DEVICE_NOT_FOUND)
                }
                is Result.Success -> {
                    logger.i(LOG_KEY, "CON - $name - Conexion exitosa!")
                    _status.update {
                        BLEUpgradeConnectionStatus.Connected(
                            BLEScannedDevice(
                                name = bleDevice.name,
                                mac = bleDevice.mac
                            )
                        )
                    }
                    observeConnection()
                    return@withContext Result.Success(Unit)
                }
            }

        }
    }

    private fun <T: BLEUpgradeConnectionError>cancelConnect(reason: T): Result.Fail<Unit, T> {
        clear()
        _status.update {
            BLEUpgradeConnectionStatus.Disconnected(
                BLEUpgradeDisconnectReason.Upgrade(reason)
            )
        }
        return Result.Fail(reason)
    }

    private var observeConnectionJob: Job? = null
    private fun observeConnection() {
        observeConnectionJob?.cancel()
        reconnectJob?.cancel()
        observeConnectionJob = coroutineScope.launch {
            logger.d(LOG_KEY, "OBS - Observando conexion")
            coroutineScope {
                bleController.device.collectLatest { tDevice->
                    tDevice
                        ?.status
                        ?.filterIsInstance<BLEDeviceStatus.Disconnected>()
                        ?.filter { it.reason != BLEDisconnectionReason.DISCONNECTED }
                        ?.distinctUntilChanged()
                        ?.collectLatest {_->
                            logger.e(LOG_KEY, "OBS - Desconexion detectada! Intentando reconectar..")
                            reconnect(tDevice)
                        }
                }
            }
        }
    }

    private var reconnectJob: Job? = null
    private fun reconnect(device: BLEDevice) {
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {
            _status.update {
                BLEUpgradeConnectionStatus.Reconnecting(
                    BLEScannedDevice(
                        device.name,
                        device.mac
                    ),
                    System.currentTimeMillis()
                )
            }
            while(isActive) {
                delay(1000)
                logger.d(LOG_KEY, "OBS - Intento de reconexion..")
                val result = bleController.device.value?.connect()
                if(result is Result.Success) {
                    logger.i(LOG_KEY, "OBS - Reconectado!")
                    _status.update {
                        BLEUpgradeConnectionStatus.Connected(
                            BLEScannedDevice(
                                device.name,
                                device.mac
                            )
                        )
                    }
                    break
                }
            }
        }
    }

    override fun disconnect() {
        logger.i(LOG_KEY, "DES - Desconectando..")
        clear()
        _status.update {
            BLEUpgradeConnectionStatus.Disconnected(
                BLEUpgradeDisconnectReason.BLE(BLEDisconnectionReason.DISCONNECTED)
            )
        }
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