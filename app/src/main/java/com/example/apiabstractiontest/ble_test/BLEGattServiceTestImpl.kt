package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.gatt.service.BLEGattService
import java.util.UUID

class BLEGattServiceTestImpl(
    override val uuid: UUID
): BLEGattService