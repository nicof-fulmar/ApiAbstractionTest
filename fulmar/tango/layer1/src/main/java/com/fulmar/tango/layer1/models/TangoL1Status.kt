package com.fulmar.tango.layer1.models

sealed interface TangoL1Status {
    data object Disconnected: TangoL1Status
    data object Connecting: TangoL1Status
    data object Connected: TangoL1Status
    data object Reconnecting: TangoL1Status
}