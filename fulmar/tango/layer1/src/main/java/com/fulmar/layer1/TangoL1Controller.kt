package com.fulmar.layer1

import com.supermegazinc.ble_upgrade.BLEUpgradeController

class TangoL1Controller(
    private val bleUpgradeController: BLEUpgradeController
) {

    val status = bleUpgradeController.status

}