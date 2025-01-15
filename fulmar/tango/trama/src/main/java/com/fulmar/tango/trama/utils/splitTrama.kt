package com.fulmar.tango.trama.utils

import com.fulmar.tango.trama.controllers.TramaControllerImpl
import com.fulmar.tango.trama.tramas.Header
import com.fulmar.tango.trama.tramas.HeaderUI
import com.fulmar.tango.trama.tramas.toUI

fun splitTrama(payload: List<Byte>): Pair<HeaderUI, List<Byte>>? {
    val header = Header()
    return TramaControllerImpl().deserialize(payload, header).takeIf{it}?.let { _->
        header.toUI().takeIf{it!=null}?.let {
            Pair(it, payload.subList(6,payload.size))
        }
    }
}