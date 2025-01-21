package com.fulmar.tango.layer1

import com.fulmar.tango.layer1.models.TangoL1Status
import com.fulmar.tango.layer1.models.TangoL1Telemetry
import com.fulmar.tango.trama.tramas.TramaTx
import kotlinx.coroutines.flow.StateFlow

interface TangoL1Controller {

    val status: StateFlow<TangoL1Status>

    val telemetry: StateFlow<TangoL1Telemetry>

    fun connect(name: String)

    suspend fun sendTelemetry(trama: TramaTx)

    fun disconnect()

}