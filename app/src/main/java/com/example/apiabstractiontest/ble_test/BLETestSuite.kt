package com.example.apiabstractiontest.ble_test

import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.log

class BLETestSuite(
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TEST-SUITE"
    }

    val adapterOff = MutableSharedFlow<Unit>()
    fun triggerAdapterOff() {
        coroutineScope.launch {
            logger.i(LOG_KEY, "triggerAdapterOff")
            adapterOff.emit(Unit)
        }
    }

    val adapterOn = MutableSharedFlow<Unit>()
    fun triggerAdapterOn() {
        coroutineScope.launch {
            logger.i(LOG_KEY, "triggerAdapterOn")
            adapterOff.emit(Unit)
        }
    }

    val connectionLost = MutableSharedFlow<Unit>()
    fun triggerConnectionLost() {
        coroutineScope.launch {
            logger.i(LOG_KEY, "triggerConnectionLost")
            connectionLost.emit(Unit)
        }
    }

}