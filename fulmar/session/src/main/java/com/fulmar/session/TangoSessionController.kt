package com.fulmar.session

import com.fulmar.session.model.TangoSession
import com.supermegazinc.escentials.Status
import kotlinx.coroutines.flow.StateFlow

interface TangoSessionController {
    val session: StateFlow<Status<TangoSession>>
}