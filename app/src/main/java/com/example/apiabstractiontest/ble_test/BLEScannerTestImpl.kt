package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.scanner.BLEScanner
import com.supermegazinc.ble.scanner.model.BLEScannedDevice
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class BLEScannerTestImpl(
    private val device: BLEScannedDevice,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
): BLEScanner {

    companion object {
        private const val LOG_KEY = "BLE-SCANNER"
    }

    private val _scannedDevices = MutableStateFlow<List<BLEScannedDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BLEScannedDevice>>
        get() = _scannedDevices.asStateFlow()

    override fun clear() {
        _scannedDevices.update { emptyList() }
    }

    override fun start(serviceUUID: List<UUID>?) {
        logger.d(LOG_KEY, "Comenzando escaneo..")
        logger.d(LOG_KEY, "Escaneo comenzado")

        coroutineScope.launch {
            delay(1000)
            logger.d(
                LOG_KEY,
                "Nuevo dispositivo: " +
                        "\nNombre: ${device.name.toString()}" +
                        "\nMac: ${device.mac}"
            )
            _scannedDevices.update { it + device }
        }

    }

    override fun stop() {
        logger.d(LOG_KEY, "Deteniendo escaneo")
        logger.d(LOG_KEY, "Escaneo detenido")
    }
}