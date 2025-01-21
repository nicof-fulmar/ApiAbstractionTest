package com.example.apiabstractiontest

import android.app.Application
import com.example.apiabstractiontest.ble_test.BLEControllerTestImpl
import com.example.apiabstractiontest.ble_test.BLETestK
import com.example.apiabstractiontest.ble_test.BLETestSuite
import com.example.apiabstractiontest.ble_test.CryptographyControllerTestImpl
import com.example.apiabstractiontest.ble_test.TangoL1ControllerTestImpl
import com.example.apiabstractiontest.ble_test.TangoL1ControllerTestPiolaImpl
import com.fulmar.tango.layer1.TangoL1Controller
import com.fulmar.tango.session.TangoSessionController
import com.fulmar.tango.session.TangoSessionControllerImpl
import com.fulmar.tango.trama.controllers.TramaControllerImpl
import com.supermegazinc.ble.BLEController
import com.supermegazinc.ble.BLEControllerImpl
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.BLEUpgradeControllerImpl
import com.supermegazinc.logger.Logger
import com.supermegazinc.logger.LoggerImpl
import com.supermegazinc.security.cryptography.CryptographyControllerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class App: Application() {
    companion object {
        lateinit var logger: Logger
        lateinit var bleController: BLEController
        lateinit var bleUpgrade: BLEUpgradeController
        lateinit var tangoSession: TangoSessionController
        lateinit var tangoL1Controller: TangoL1Controller
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
        val cryptographyController = CryptographyControllerImpl(
            applicationContext,
            logger
        )

        //val bleTestSuite = BLETestSuite(logger,coroutineScope)
        //bleController = BLEControllerTestImpl(
        //    name = BLETestK.TANGO_BLE_NAME,
        //    bleTestSuite,
        //    coroutineScope
        //)
        //val cryptographyController = CryptographyControllerTestImpl(
        //    applicationContext,
        //    logger
        //)

        bleUpgrade = BLEUpgradeControllerImpl(
            bleController,
            logger = logger,
            coroutineScope
        )

        tangoSession = TangoSessionControllerImpl(
            logger
        )

        /*
               tangoL1Controller = TangoL1ControllerImpl(
            bleUpgrade,
            cryptographyController,
            tangoSession,
            TramaControllerImpl(),
            logger,
            coroutineScope
        )
         */


        tangoL1Controller = TangoL1ControllerTestPiolaImpl(
            bleUpgrade,
            cryptographyController,
            tangoSession,
            TramaControllerImpl(),
            logger,
            coroutineScope,
            context = applicationContext
        )
    }
}