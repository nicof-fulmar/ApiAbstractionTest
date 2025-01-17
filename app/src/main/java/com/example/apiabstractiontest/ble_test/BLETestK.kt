package com.example.apiabstractiontest.ble_test

import java.util.UUID

object BLETestK {
    const val TANGO_BLE_NAME = "TAXI-PRUEBA1-"
    val SERVICE_MAIN_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914c")
    val CHARACTERISTIC_RECEIVE_KEY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")
    val CHARACTERISTIC_SEND_KEY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa")
    val CHARACTERISTIC_RECEIVE_TELEMETRY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a7")
    val CHARACTERISTIC_SEND_TELEMETRY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    val CHARACTERISTIC_RECEIVE_PROGRAMACION_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26ad")
    val TANGO_PUBLIC_KEY: ByteArray = listOf<Byte>(4,-128,3,71,38,-94,76,52,-91,-53,-107,-65,96,-48,-117,-43,1,-62,-66,-46,60,-27,17,54,-76,59,-6,-24,54,106,-22,-47,-40,111,-45,62,114,39,-78,-106,117,-126,4,70,110,116,37,3,75,54,-68,-14,29,-81,-23,-62,-72,85,-98,78,102,-91,74,-39,21,113,46,-53,-126,-1,-61,-69,113,-2,-107,98,111,63,123,-70,32,30,9,-2,111,81,-9,119,-5,-88,-104,119,-44,-1,34,-92,103,-89,-31,-12,123,4,91,109,-104,-59,89,-105,20,-11,38,-127,62,-93,-111,-102,-65,61,21,35,-76,17,40,-21,-76,-85,-39,29,70,-48,55,26,71,75,115,-128,-116,-7,-99,-100,-28,-46,-97,1,93,-63,-88,43,53,14,56,-33,41,105,25,-109,-23,-111,-80,118,115,-93,-38,-94,-106,53,-70,-86,3,91,5,115,29,-56,127,-127,-124,-37,-79,-59,-110,49,-49,101,74,-9,-93,-115,-37,-10,-27,-117,75,108,28,-58,74,-62,-92,88,-123,37,88,-35,38,21,-66,-98,-42,-7,-12,-102,92,7,-34,53,-65,45,-66,122,16,-47,106,-52,-40,-88,81,56,-13,-95,34,47,-128,-36,27,-92,-36,-71,-96,53,13,104,-39,-63,78,-91,40,93,-39,120,51,-9,-91,-56,37,109,-55,-58,67,46,-27,-103,-84,-93,-6,-92,94,-113,117,-4,-10,-26,62,57,39,98,111,118,121,120,-107,12,-77,-62,98,3,70,92,-8,-4,-15,35,-10,78,8,73,-105,21,117,89,90,29,33,83,4,122,-42,15,72,-126,3,50,-8,-65,70,89,92,-11,72,-58,12).toByteArray()
    val TANGO_SHARED_KEY: ByteArray = listOf<Byte>(68, -61, 102, 43, 44, -8, 113, -36, -121, -115, 71, -61, -112, 57, 48, 84, 71, 88, -108, -123, -74, -60, -116, -101, 93, 63, -61, -8, -2, 42, 53, 81).toByteArray()
    val TANGO_TELEMETRY_PAYLOAD: ByteArray = listOf<Byte>(-27,-107,48,-104,59,-60,87,-57,-18,-70,125,-27,-117,-45,-67,91,46,18,-117,-54,66,-112,-43,-106,-67,25,-110,117,88,95,-118,3,10,-122,-106,-20,88,-98,-70,117,-97,59,-124,-9,-86,-121,18,-72,-80,-97,118,-63,55,89,-76,-65,-26,67,-63,-116,-36,-6,-42,29,-7,81,-94,25,-59,15,8,-44,-122,-121,-121,-103,-99,-14,71,-127,-56,18,-24,-8,-92,-78,-126,115,30,-28,50,25,-4,117,64,-119,74,-105,106,-35,-127,-33,-126,50,-112,78,-65,-107,-15,-85,20,24).toByteArray()
}