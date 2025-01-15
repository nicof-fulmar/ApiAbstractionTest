package com.fulmar.tango.trama.tramas

import com.fulmar.tango.trama.mappers.toSerialByteArray
import kotlin.experimental.xor

data class RimpTx (
    val open:           Byte = '<'.code.toByte(),
    val type:           List<Byte> = "RIMP".toByteArray().toList(),
    val way:            List<Byte> = "VDT-FM".toByteArray().toList(),
    val status:         Byte = 1,
    var state:          Byte = 0,
    var serial:         MutableList<Byte> = MutableList(8){0},
    val packetNumber:   Byte = 1,
    var checksum:       Byte = 0,
    val close:          Byte = '>'.code.toByte(),
) : TramaTx

data class RimpTxUI(
    var state: Char = ' ',
    var serial: String = "",
)

fun RimpTx.checksum() : Byte {
    val bytesToXOR = mutableListOf<Byte>()
    bytesToXOR.add(open)
    bytesToXOR.addAll(type)
    bytesToXOR.addAll(way)
    bytesToXOR.add(status)
    bytesToXOR.add(state)
    bytesToXOR.addAll(serial)
    bytesToXOR.add(packetNumber)
    var checksum: Byte = 0
    for (byte in bytesToXOR) {
        checksum = checksum xor byte
    }
    return checksum
}


fun RimpTxUI.toTrama() : RimpTx? {
    return try {
        RimpTx(
            state = state.code.toByte(),
            serial = serial.toSerialByteArray(8)!!.toMutableList(),
        ).apply {
            checksum = checksum()
        }
    } catch (_:Exception) {
        null
    }
}