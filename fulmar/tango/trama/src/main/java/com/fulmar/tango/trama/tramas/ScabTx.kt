package com.fulmar.tango.trama.tramas

import com.fulmar.tango.trama.controllers.TramaControllerImpl
import kotlin.experimental.xor

data class ScabTx(
    val open:           Byte = '<'.code.toByte(),
    val type:           List<Byte> = "SCAB".toByteArray().toList(),
    val way:            List<Byte> = "VDT-FM".toByteArray().toList(),
    val status:         Byte = 1,
    var state:          Byte = 0,
    val packetNumber:   Byte = 1,
    var checksum:       Byte = 0,
    val close:          Byte = '>'.code.toByte(),
) : TramaTx

data class ScabTxUI(
    var state: Char = ' ',
)

fun ScabTx.checksum(): Byte {
    val bytesToXOR = mutableListOf<Byte>()
    bytesToXOR.add(open)
    bytesToXOR.addAll(type)
    bytesToXOR.addAll(way)
    bytesToXOR.add(status)
    bytesToXOR.add(state)
    bytesToXOR.add(packetNumber)
    var checksum: Byte = 0
    for (byte in bytesToXOR) {
        checksum = checksum xor byte
    }
    return checksum
}

fun ScabTxUI.toTrama(): ScabTx {
    return ScabTx().apply {
        state = this@toTrama.state.code.toByte()
        checksum = checksum()
    }
}

fun List<Byte>.toScabTxUI(): ScabTxUI? {
    val res = ScabTx()
    return if(TramaControllerImpl().deserialize(this, res)) ScabTxUI(res.state.toInt().toChar())
    else null
}