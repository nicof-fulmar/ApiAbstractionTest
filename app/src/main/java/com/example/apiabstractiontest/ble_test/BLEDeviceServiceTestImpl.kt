package com.example.apiabstractiontest.ble_test

import com.supermegazinc.ble.device.service.BLEDeviceService
import java.util.UUID

class BLEDeviceServiceTestImpl(
    private val _uuid: UUID
): BLEDeviceService {
    override val uuid: UUID
        get() = _uuid
}