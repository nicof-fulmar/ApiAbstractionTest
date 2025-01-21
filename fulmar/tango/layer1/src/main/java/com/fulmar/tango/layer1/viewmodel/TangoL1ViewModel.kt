package com.fulmar.tango.layer1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fulmar.tango.layer1.TangoL1Controller
import com.fulmar.tango.layer1.models.TangoL1TelemetryUI
import com.fulmar.tango.trama.tramas.ScabRxUI
import com.fulmar.tango.trama.tramas.SimpRxSummaryUI
import com.fulmar.tango.trama.tramas.TramaTx
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TangoL1ViewModel(
    private val controller: TangoL1Controller
): ViewModel() {

    val status = controller.status
    val telemetry = controller.telemetry.map {
        TangoL1TelemetryUI(
            simpSummary = it.simpSummary ?: SimpRxSummaryUI(),
            scab = it.scab ?: ScabRxUI()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        TangoL1TelemetryUI()
    )

    fun sendTelemetry(payload: TramaTx) {
        viewModelScope.launch {
            controller.sendTelemetry(payload)
        }
    }

}