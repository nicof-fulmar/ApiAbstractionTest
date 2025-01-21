package com.fulmar.firmware.util

fun ByteArray.dividePacket(chunkSize: Int): List<ByteArray> {
    return (indices step chunkSize).map { start ->
        this.copyOfRange(start, (start + chunkSize).coerceAtMost(this.size))
    }
}