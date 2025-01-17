package com.fulmar.tango.trama.tramas

import com.fulmar.tango.trama.models.Commands

data class Header (
    val open:           Byte = '<'.code.toByte(),
    val packetNumber:   MutableList<Byte> = MutableList(2){0},
    val cmd:            MutableList<Byte> = MutableList(2){0},
    val close:          Byte = '>'.code.toByte(),
)

data class HeaderUI(
    var packetNumber: Int = 0,
    var cmd: Commands = Commands.RELAY
)

fun HeaderUI.toTrama() : Header? {
    return try {
        Header().apply {
            packetNumber[0] = ((this@toTrama.packetNumber shl 8) and 255).toByte()
            packetNumber[1] = (this@toTrama.packetNumber and 255).toByte()
            cmd[0] = ((this@toTrama.cmd.code shl 8) and 255).toByte()
            cmd[1] = (this@toTrama.cmd.code and 255).toByte()
        }
    } catch (_:Exception) {
        null
    }
}

fun Header.toUI() : HeaderUI? {
    return try {
        HeaderUI(
            packetNumber = ((this.packetNumber[0].toLong() shl 8) or this.packetNumber[1].toLong()).toInt(),
            cmd = Commands.entries.first { ((this.cmd[0].toLong() shl 8) or this.cmd[1].toLong()).toInt() == it.code}
        )
    } catch (_:Exception) {
        null
    }
}