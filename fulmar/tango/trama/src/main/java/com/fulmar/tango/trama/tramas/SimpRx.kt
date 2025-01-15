package com.fulmar.tango.trama.tramas

import com.fulmar.tango.trama.controllers.TramaControllerImpl
import kotlin.experimental.xor
import kotlin.math.pow

data class SimpRx (
    val open:               Byte = '<'.code.toByte(),
    val type:               List<Byte> = "SIMP".toByteArray().toList(),
    val way:                List<Byte> = "FM-VDT".toByteArray().toList(),
    var bytesQty:           Byte = 0,
    val serial:             MutableList<Byte> = MutableList(7){32},
    val plate:              MutableList<Byte> = MutableList(9){32},
    val license:            MutableList<Byte> = MutableList(7){32},
    val wheelSize:          MutableList<Byte> = MutableList(10){32},
    val travelId:           MutableList<Byte> = MutableList(2){0},
    var priceType:          Byte = 0,
    val initialPrice:       MutableList<Byte> = MutableList(4){0},
    val tokenPrice:         MutableList<Byte> = MutableList(2){0},
    val metersPerToken:     MutableList<Byte> = MutableList(2){0},
    val travelStart:        MutableList<Byte> = MutableList(2){0},
    val travelEnd:          MutableList<Byte> = MutableList(5){0},
    val distanceKm:         MutableList<Byte> = MutableList(3){0},
    var maxSpeed:           Byte = 0,
    val waitTime:           MutableList<Byte> = MutableList(2){0},
    val tokenQty:           MutableList<Byte> = MutableList(2){0},
    val priceExtra:         MutableList<Byte> = MutableList(4){0},
    val price:              MutableList<Byte> = MutableList(4){0},
    var priceDecimal:       Byte = 0,
    var state:              Byte = 0,
    var speed:              Byte = 0,
    var packetNumber:       Byte = 0,
    var checksum:           Byte = 0,
    val close:              Byte = '>'.code.toByte()
)

data class SimpRxUI (
    val serial:             String,
    val plate:              String,
    val license:            String,
    val wheelSize:          String,
    val travelId:           Long,
    val priceType:          Int,
    val initialPrice:       Int,
    val tokenPrice:         Int,
    val metersPerToken:     Int,
    val startHour:          Int,
    val startMinute:        Int,
    val endDay:             Int,
    val endMonth:           Int,
    val endYear:            Int,
    val endHour:            Int,
    val endMinute:          Int,
    val distanceKm:         Float,
    val maxSpeed:           Int = 0,
    val waitTime:           Int,
    val tokenQty:           Int,
    val priceExtra:         Float,
    val price:              Float,
    val state: SimpState,
    val speed:              Int
)

data class SimpRxSummaryUI (
    var travelId: Int = 0,
    var price: Float = 0f,
    var kilometers: Float = 0f,
    var speed: Int = 0,
    var maxSpeed: Int = 0,
    var priceExtra: Float = 0f,
    var priceType: Int = 0,
    var state: SimpState = SimpState.UNKNOWN,
    var serial: String = "",
)

data class SimpRxTicketUI (
    var serial: String,
    var plate: String,
    var license: String,
    var wheelSize: String,
    var travelId: Int,
    var priceType: Int,
    var initialPrice: Int,
    var tokenPrice: Int,
    var metersPerToken: Int,
    var startHour: Int,
    var startMinute: Int,
    var endDay: Int,
    var endMonth: Int,
    var endYear: Int,
    var endHour: Int,
    var endMinute: Int,
    var distanceKm: Float,
    var maxSpeed: Int,
    var waitMinutes: Int,
    var tokenQty: Int,
    var extras: Float,
    var price: Float
)

private fun intToBigEndianBytes(value: Int, length: Int): ByteArray {
    val byteArray = ByteArray(length)
    for (i in 0 until length) {
        byteArray[length - 1 - i] = ((value shr (8 * i)) and 0xFF).toByte()
    }
    return byteArray
}

fun bigEndianBytesToInt(list: List<Byte>): Int {
    var result = 0
    for (byte in list) {
        result = (result shl 8) or (byte.toInt() and 0xFF)
    }
    return result
}

private fun addToEnd(list: MutableList<Byte>, payload: List<Byte>) {
    if(list.size>=payload.size) {
        for(i in 1..payload.size) {
            list[list.size-i] = payload[payload.size-i]
        }
    }
}

//Esto solo usa en moto test para simular lo que enviaria el taximetro
fun SimpRxUI.toSimpRx(): SimpRx {
    return SimpRx(
        serial =  MutableList<Byte>(7){32}.apply {
            addToEnd(this, this@toSimpRx.serial.toByteArray().toList())
        },
        plate = MutableList<Byte>(9){32}.apply {
            addToEnd(this, this@toSimpRx.plate.toByteArray().toList())
        },
        license = MutableList<Byte>(7){32}.apply {
            addToEnd(this, this@toSimpRx.license.toByteArray().toList())
        },
        wheelSize = MutableList<Byte>(10){32}.apply {
            addToEnd(this, this@toSimpRx.wheelSize.toByteArray().toList())
        },
        travelId = MutableList<Byte>(2){0}.apply {
            addToEnd(this, intToBigEndianBytes(travelId.toInt(),2).toList())
        },
        metersPerToken = MutableList<Byte>(2){0}.apply {
            addToEnd(this, intToBigEndianBytes(metersPerToken,2).toList())
        },
        travelStart = MutableList<Byte>(2){0}.apply {
            this[0] = startHour.toByte()
            this[1] = startMinute.toByte()
        },
        travelEnd = MutableList<Byte>(5){0}.apply {
            this[0] = endDay.toByte()
            this[1] = endMonth.toByte()
            this[2] = endYear.toByte()
            this[3] = endHour.toByte()
            this[4] = endMinute.toByte()
        },
        distanceKm = MutableList<Byte>(3){0}.apply {
            distanceKm.toInt().let {tKm->
                (distanceKm - tKm).toInt().let {tM->
                    intToBigEndianBytes(tKm,2).toList().also {
                        this[0] = it[0]
                        this[1] = it[1]
                    }
                    this[2] = tM.toByte()
                }
            }
        },
        maxSpeed = maxSpeed.toByte(),
        waitTime = MutableList<Byte>(2){0}.apply {
            addToEnd(this, intToBigEndianBytes(waitTime,2).toList())
        },
        tokenQty = MutableList<Byte>(2){0}.apply {
            addToEnd(this, intToBigEndianBytes(tokenQty,2).toList())
        },
        priceExtra = MutableList<Byte>(4){0},
        price = MutableList<Byte>(4){0}.apply {
            addToEnd(this, intToBigEndianBytes((price * 10.0.pow(2.toDouble()).toFloat()).toInt(),4).toList())
        },
        priceDecimal = 2.toByte(),
        state = when(state) {
            SimpState.FREE -> 'L'.code.toByte()
            SimpState.BUSY -> 'O'.code.toByte()
            SimpState.STOPPED -> 'D'.code.toByte()
            SimpState.OFF -> 'P'.code.toByte()
            SimpState.UNKNOWN -> 'R'.code.toByte()
        },
        speed = speed.toByte(),
    ).let { tList->
        tList.copy(
            checksum = tList.checksum()
        )
    }
}

enum class SimpState {
    FREE,
    BUSY,
    STOPPED,
    OFF,
    UNKNOWN
}

fun SimpRx.checksum(): Byte {
    val bytesToXOR = mutableListOf<Byte>()
    bytesToXOR.add(open)
    bytesToXOR.addAll(type)
    bytesToXOR.addAll(way)
    bytesToXOR.add(bytesQty)
    bytesToXOR.addAll(serial)
    bytesToXOR.addAll(plate)
    bytesToXOR.addAll(license)
    bytesToXOR.addAll(wheelSize)
    bytesToXOR.addAll(travelId)
    bytesToXOR.add(priceType)
    bytesToXOR.addAll(initialPrice)
    bytesToXOR.addAll(tokenPrice)
    bytesToXOR.addAll(metersPerToken)
    bytesToXOR.addAll(travelStart)
    bytesToXOR.addAll(travelEnd)
    bytesToXOR.addAll(distanceKm)
    bytesToXOR.add(maxSpeed)
    bytesToXOR.addAll(waitTime)
    bytesToXOR.addAll(tokenQty)
    bytesToXOR.addAll(priceExtra)
    bytesToXOR.addAll(price)
    bytesToXOR.add(priceDecimal)
    bytesToXOR.add(state)
    bytesToXOR.add(speed)
    bytesToXOR.add(packetNumber)
    var checksum: Byte = 0
    for (byte in bytesToXOR) {
        checksum = checksum xor byte
    }
    return checksum
}

fun List<Byte>.toSimpRx(): SimpRx? {
    return SimpRx().apply {
        TramaControllerImpl().deserialize(this@toSimpRx, this).also {
            if(!it || checksum() != checksum) return null
        }
    }
}

fun SimpRx.toUI() : SimpRxSummaryUI {

    val tPrice = try{ bigEndianBytesToInt(price) } catch (_: Exception) {0}
    val tPriceExtra = try{  bigEndianBytesToInt(priceExtra) } catch (_: Exception) {0}
    val tDecimals = try{ priceDecimal.toInt() } catch (_: Exception) {0}
    val tDistanceKm = try{ bigEndianBytesToInt(distanceKm.subList(0,2)) } catch (_: Exception) {0}
    val tDistanceM = try{ distanceKm[2].toInt()} catch (_: Exception) {0}
    val tspeed = try{ speed.toInt() } catch (_: Exception) {0}

    return SimpRxSummaryUI(
        travelId = try{  bigEndianBytesToInt(travelId)
        } catch (_: Exception) {0},
        price = if(tDecimals>0) tPrice / 10.0.pow(tDecimals).toFloat() else tPrice.toFloat(),
        kilometers = tDistanceKm.toFloat() + (tDistanceM.toFloat() / 10f),
        //kilometers = tDistanceKm.toFloat() + tDistanceM.toFloat(),
        speed = try{ speed.toInt()} catch (_: Exception) {0},
        maxSpeed = try{ maxSpeed.toInt()} catch (_: Exception) {0},
        priceExtra = if(tDecimals>0) tPriceExtra / 10.0.pow(tDecimals).toFloat() else tPriceExtra.toFloat(), //TODO WTF
        priceType = try{ priceType.toInt()} catch (_: Exception) {0},
        state = try {when(state.toInt().toChar()) {
            'L' -> SimpState.FREE
            'O' -> SimpState.BUSY
            'D' -> SimpState.STOPPED
            'P' -> SimpState.OFF
            else -> SimpState.UNKNOWN
        }} catch (_: Exception) {
            SimpState.UNKNOWN
        },
        serial = try { serial.toByteArray().decodeToString().trim() } catch (_: Exception) {""}
    )
}

fun SimpRx.toTicketUI(): SimpRxTicketUI {

    val tPrice = try{ bigEndianBytesToInt(price) } catch (_: Exception) {0}
    val tPriceExtra = try{  bigEndianBytesToInt(priceExtra) } catch (_: Exception) {0}
    val tDecimals = try{ priceDecimal.toInt() } catch (_: Exception) {0}
    val tDistanceKm = try{ bigEndianBytesToInt(distanceKm.subList(0,1)) } catch (_: Exception) {0}
    val tDistanceM = try{ distanceKm[2].toInt()} catch (_: Exception) {0}

    return SimpRxTicketUI(
        serial = try { serial.toByteArray().decodeToString().trim() } catch (_: Exception) {""},
        plate = try { plate.toByteArray().decodeToString().trim() } catch (_: Exception) {""},
        license = try { license.toByteArray().decodeToString().trim() } catch (_: Exception) {""},
        wheelSize = try { license.toByteArray().decodeToString().trim() } catch (_: Exception) {""},
        travelId = try{  bigEndianBytesToInt(travelId)
        } catch (_: Exception) {0},
        priceType = try{ priceType.toInt()} catch (_: Exception) {0},
        initialPrice = try{  bigEndianBytesToInt(initialPrice) } catch (_: Exception) {0},
        tokenPrice = try{  bigEndianBytesToInt(tokenPrice) } catch (_: Exception) {0},
        metersPerToken = try{  bigEndianBytesToInt(metersPerToken) } catch (_: Exception) {0},
        startHour = try{ travelStart[0].toInt() } catch (_: Exception) {0},
        startMinute = try{ travelStart[1].toInt() } catch (_: Exception) {0},
        endDay = try{ travelEnd[0].toInt() } catch (_: Exception) {0},
        endMonth = try{ travelEnd[1].toInt() } catch (_: Exception) {0},
        endYear = try{ travelEnd[2].toInt() } catch (_: Exception) {0},
        endHour = try{ travelEnd[3].toInt() } catch (_: Exception) {0},
        endMinute = try{ travelEnd[4].toInt() } catch (_: Exception) {0},
        distanceKm = tDistanceKm.toFloat() + tDistanceM.toFloat() / 10f,
        maxSpeed = try{ maxSpeed.toInt() } catch (_: Exception) {0},
        waitMinutes = try{  bigEndianBytesToInt(waitTime)
        } catch (_: Exception) {0},
        tokenQty = try{ bigEndianBytesToInt(tokenQty) } catch (_: Exception) {0},
        extras = if(tDecimals>0) tPriceExtra / 10.0.pow(tDecimals).toFloat() else tPriceExtra.toFloat(),
        price = if(tDecimals>0) tPrice / 10.0.pow(tDecimals).toFloat() else tPrice.toFloat(),
    )
}