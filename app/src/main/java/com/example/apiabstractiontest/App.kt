package com.example.apiabstractiontest

import android.app.Application
import com.fulmar.tango.session.TangoSessionController
import com.fulmar.tango.session.TangoSessionControllerImpl
import com.supermegazinc.ble.BLEController
import com.supermegazinc.ble.BLEControllerImpl
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.BLEUpgradeControllerImpl
import com.supermegazinc.diffie_hellman.DiffieHellmanController
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID

class App: Application() {
    companion object {
        lateinit var logger: Logger
        lateinit var bleController: BLEController
        lateinit var bleUpgrade: BLEUpgradeController
        lateinit var diffieHellman: DiffieHellmanController
        lateinit var tangoSession: TangoSessionController
    }

    override fun onCreate() {
        super.onCreate()

        logger = Logger()

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

        diffieHellman = DiffieHellmanController(
            logger
        )

        tangoSession = TangoSessionControllerImpl(
            bleUpgrade,
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9"),
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa"),
            diffieHellman,
            logger,
            coroutineScope
        )
    }
}