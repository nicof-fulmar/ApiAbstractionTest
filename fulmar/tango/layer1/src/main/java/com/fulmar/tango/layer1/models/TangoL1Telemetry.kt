package com.fulmar.tango.layer1.models

import com.fulmar.tango.trama.tramas.ScabRxUI
import com.fulmar.tango.trama.tramas.SimpRxSummaryUI
import com.fulmar.tango.trama.tramas.SimpRxTicketUI

data class TangoL1Telemetry(
    val simpSummary: SimpRxSummaryUI?=null,
    val simpTicket: SimpRxTicketUI?=null,
    val scab: ScabRxUI?=null
)