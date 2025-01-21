package com.fulmar.tango.layer1.models

import com.fulmar.tango.trama.tramas.ScabRxUI
import com.fulmar.tango.trama.tramas.SimpRxSummaryUI
import com.fulmar.tango.trama.tramas.SimpRxTicketUI

data class TangoL1TelemetryUI(
    val simpSummary: SimpRxSummaryUI = SimpRxSummaryUI(),
    val scab: ScabRxUI = ScabRxUI()
)