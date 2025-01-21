package com.fulmar.tango.layer1.config

import java.util.UUID

object TangoL1Config {
    val SERVICE_MAIN_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914c")
    val CHARACTERISTIC_RECEIVE_KEY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")
    val CHARACTERISTIC_SEND_KEY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa")
    val CHARACTERISTIC_RECEIVE_TELEMETRY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a7")
    val CHARACTERISTIC_SEND_TELEMETRY_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    val CHARACTERISTIC_RECEIVE_PROGRAMACION: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26ad")
    val CHARACTERISTIC_RECEIVE_FIRMWARE: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26af")
    val CHARACTERISTIC_SEND_FIRMWARE: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26ae")

    const val CONNECTION_TIMEOUT = Long.MAX_VALUE //TODO: Emprolijar
}