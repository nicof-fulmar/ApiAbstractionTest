package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.device.service.BLEDeviceService
import java.util.UUID

class BLEDeviceServiceTestImpl(
    override val uuid: UUID
): BLEDeviceService