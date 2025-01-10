package com.fulmar.tango.session

import com.fulmar.tango.session.model.TangoSession
import com.supermegazinc.escentials.Result
import com.supermegazinc.escentials.Status
import kotlinx.coroutines.flow.StateFlow

interface TangoSessionController {
    val session: StateFlow<Status<TangoSession>>
    suspend fun refreshAndGetPublicKey(): List<Byte>
    suspend fun generateSession(peerPublicKey: List<Byte>): Result<TangoSession, Unit>
}