package com.fulmar.tango.trama.tramas

import kotlin.experimental.xor

data class ShabTx(
    val open:           Byte = '<'.code.toByte(),
    val type:           List<Byte> = "SHAB".toByteArray().toList(),
    val way:            List<Byte> = "VDT-FM".toByteArray().toList(),
    val status:         Byte = 1,
    var state:          Byte = 0,
    val packetNumber:   Byte = 1,
    var checksum:       Byte = 0,
    val close:          Byte = '>'.code.toByte(),
) : TramaTx

data class ShabTxUI(
    var state: Int = 0,
)

fun ShabTx.checksum(): Byte {
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

fun ShabTxUI.toTrama(): ShabTx {
    return ShabTx().apply {
        state = (this@toTrama.state + '0'.code).toChar().code.toByte()
        checksum = checksum()
    }
}