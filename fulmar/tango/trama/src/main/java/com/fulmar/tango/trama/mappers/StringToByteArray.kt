package com.fulmar.tango.trama.mappers

fun String.toSerialByteArray(len: Int): ByteArray? {
    val serial = this.toByteArray()
    val serialRawContainer: MutableList<Byte> = MutableList(len){32}
    if(serial.size > serialRawContainer.size) return null
    var posSerial: Int = serial.size
    var posContainer: Int = serialRawContainer.size
    while(posSerial>0) {
        serialRawContainer[posContainer - 1] = serial[posSerial - 1]
        posSerial--
        posContainer--
    }
    return serialRawContainer.toByteArray()
}