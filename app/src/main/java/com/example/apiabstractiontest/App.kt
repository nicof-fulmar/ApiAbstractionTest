package com.example.apiabstractiontest

import android.app.Application
import com.fulmar.layer1.TangoL1Controller
import com.fulmar.layer1.TangoL1ControllerTest
import com.fulmar.tango.session.TangoSessionController
import com.fulmar.tango.session.TangoSessionControllerImpl
import com.supermegazinc.ble.BLEController
import com.supermegazinc.ble.BLEControllerImpl
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.BLEUpgradeControllerImpl
import com.supermegazinc.ble_upgrade.BLEUpgradeControllerTestImpl
import com.supermegazinc.logger.Logger
import com.supermegazinc.logger.LoggerImpl
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID

class App: Application() {
    companion object {
        lateinit var logger: Logger
        lateinit var bleController: BLEController
        lateinit var bleUpgrade: BLEUpgradeController
        lateinit var tangoSession: TangoSessionController
        lateinit var tangoL1Controller: TangoL1ControllerTest
    }

    override fun onCreate() {
        super.onCreate()

        logger = LoggerImpl()

        val coroutineScope = CoroutineScope(Dispatchers.IO)

        bleController = BLEControllerImpl(
            context = applicationContext,
            logger = logger,
            coroutineScope = coroutineScope
        )

        bleUpgrade = BLEUpgradeControllerImpl(
            bleController,
            logger = logger,
            coroutineScope
        )

        //bleUpgrade = BLEUpgradeControllerTestImpl(bleController)


        tangoSession = TangoSessionControllerImpl(
            logger
        )

        tangoL1Controller = TangoL1ControllerTest(
            bleUpgrade,
            tangoSession,
            CryptographyController(applicationContext, logger),
            logger,
            coroutineScope
        )
    }
}