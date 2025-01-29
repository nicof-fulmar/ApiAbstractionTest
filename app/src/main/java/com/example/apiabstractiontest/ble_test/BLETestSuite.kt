package com.example.apiabstractiontest.ble_test

import com.fulmar.tango.layer1.config.TangoL1Config
import com.supermegazinc.ble.gatt.model.BLEMessageEvent
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class BLETestSuite(
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TEST-BLE"
    }

    private val hiddenCharacteristics = MutableStateFlow<List<UUID>>(emptyList())
    val characteristics = MutableStateFlow<List<UUID>>(emptyList())
    val services = MutableStateFlow<List<UUID>>(emptyList())

    val lostConnectionTrigger = MutableSharedFlow<Unit>()
    fun onLostConnection() {
        coroutineScope.launch {
            lostConnectionTrigger.emit(Unit)
        }
    }

    fun onConnectGatt() {
        hiddenCharacteristics.update {
            listOf(
                TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY,
                TangoL1Config.CHARACTERISTIC_RECEIVE_KEY,
                TangoL1Config.CHARACTERISTIC_SEND_TELEMETRY,
                TangoL1Config.CHARACTERISTIC_SEND_KEY,
            )
        }
        services.update {
            listOf(
                TangoL1Config.SERVICE_MAIN_UUID
            )
        }
    }

    fun onSessionCreated() {
        hiddenCharacteristics.update {
            it +
            listOf(
                TangoL1Config.CHARACTERISTIC_RECEIVE_FIRMWARE,
                TangoL1Config.CHARACTERISTIC_RECEIVE_PROGRAMACION,
                TangoL1Config.CHARACTERISTIC_SEND_FIRMWARE,
            )
        }
    }

    val receiveMessages = MutableSharedFlow<BLEMessageEvent>()

    private var receiveTelemetryJob: Job? = null
    private fun controlReceiveTelemetry(state: Boolean) {
        receiveTelemetryJob?.cancel()
        if(state) {
            receiveTelemetryJob = coroutineScope.launch {
                while (isActive) {
                    receiveMessages.emit(BLEMessageEvent(TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY, BLETestK.TANGO_TELEMETRY_PAYLOAD))
                    delay(1000)
                }
            }
        }
    }

    fun setNotification(uuid: UUID, state: Boolean) {
        when(uuid) {
            TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY -> controlReceiveTelemetry(state)
        }
    }

    fun forceRead(uuid: UUID) {
        when(uuid) {
            TangoL1Config.CHARACTERISTIC_RECEIVE_KEY -> {
                coroutineScope.launch {
                    receiveMessages.emit(BLEMessageEvent(TangoL1Config.CHARACTERISTIC_RECEIVE_KEY, BLETestK.TANGO_PUBLIC_KEY))
                }
            }
            TangoL1Config.CHARACTERISTIC_RECEIVE_PROGRAMACION -> {
                coroutineScope.launch {
                    receiveMessages.emit(BLEMessageEvent(TangoL1Config.CHARACTERISTIC_RECEIVE_PROGRAMACION, BLETestK.TANGO_RECEIVE_PROGRAMACION_PAYLOAD))
                }
            }
        }
    }

    fun onDiscoverServices() {
        characteristics.update { hiddenCharacteristics.value }
    }

}