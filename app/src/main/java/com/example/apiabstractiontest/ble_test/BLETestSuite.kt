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

    private val _adapterOff = MutableSharedFlow<Unit>()
    val adapterOff = _adapterOff.asSharedFlow()
    fun triggerAdapterOff() {
        coroutineScope.launch {
            logger.i(LOG_KEY, "triggerAdapterOff")
            _adapterOff.emit(Unit)
        }
    }

}