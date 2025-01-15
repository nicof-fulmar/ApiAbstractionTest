package com.fulmar.tango.trama.tramas

import com.fulmar.tango.trama.controllers.TramaControllerImpl
import kotlin.experimental.xor

data class ScabRx (
    val open:           Byte = '<'.code.toByte(),
    val type:           List<Byte> = "RCAB".toByteArray().toList(),
    val way:            List<Byte> = "FM-VDT".toByteArray().toList(),
    var bytesQty:       Byte = 0,
    var state:          Byte = 0,
    var priceType:      Byte = 0,
    var packetNumber:   Byte = 0,
    var checksum:       Byte = 0,
    val close:          Byte = '>'.code.toByte()
)

data class ScabRxUI (
    var state: Char = ' ',
    var priceType: Int = 0
)

fun ScabRx.checksum(): Byte {
    val bytesToXOR = mutableListOf<Byte>().apply {
        add(open)
        addAll(type)
        addAll(way)
        add(bytesQty)
        add(state)
        add(priceType)
        add(packetNumber)
    }
    var checksum: Byte = 0
    for (byte in bytesToXOR) {
        checksum = checksum xor byte
    }
    return checksum
}

fun List<Byte>.toScabRx(): ScabRx? {
    return ScabRx().apply {
        TramaControllerImpl().deserialize(this@toScabRx, this).also {
            if(!it || checksum() != checksum) return null
        }
    }
}

fun ScabRx.toUI() : ScabRxUI {
    return ScabRxUI(
        priceType = priceType.toInt(),
        state = state.toInt().toChar(),
    )
}