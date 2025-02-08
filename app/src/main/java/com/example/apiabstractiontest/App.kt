package com.example.apiabstractiontest

import android.app.Application
import com.example.apiabstractiontest.ble_test.BLEAdapterTestImpl
import com.example.apiabstractiontest.ble_test.BLEGattControllerTestImpl
import com.example.apiabstractiontest.ble_test.BLEScannerTestImpl
import com.example.apiabstractiontest.ble_test.BLETestK
import com.example.apiabstractiontest.ble_test.BLETestSuite
import com.example.apiabstractiontest.ble_test.CryptographyControllerTestImpl
import com.example.apiabstractiontest.ble_test.TangoL1ControllerTestFirmwareImpl
import com.fulmar.api.createApiService
import com.fulmar.api.di.ApiModuleInitializer
import com.fulmar.api.model.ApiCertificateInput
import com.fulmar.firmware.TangoFirmwareController
import com.fulmar.firmware.feature_api.TangoFirmwareApiImpl
import com.fulmar.firmware.feature_api.service.TangoFirmwareApiService
import com.fulmar.tango.layer1.TangoL1Controller
import com.fulmar.tango.layer1.TangoL1ControllerImpl
import com.fulmar.tango.session.TangoSessionController
import com.fulmar.tango.session.TangoSessionControllerImpl
import com.fulmar.tango.trama.controllers.TramaControllerImpl
import com.supermegazinc.ble.BLEControllerImpl
import com.supermegazinc.ble.adapter.BLEAdapterImpl
import com.supermegazinc.ble.gatt.BLEGattControllerImpl
import com.supermegazinc.ble.scanner.BLEScannerImpl
import com.supermegazinc.ble.scanner.model.BLEScannedDevice
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
        lateinit var bleUpgrade: BLEUpgradeController
        lateinit var tangoSession: TangoSessionController
        lateinit var tangoL1Controller: TangoL1Controller
        lateinit var bleTestSuite: BLETestSuite
    }

    override fun onCreate() {
        super.onCreate()

        logger = LoggerImpl()

        ApiModuleInitializer(logger)

        val coroutineScope = CoroutineScope(Dispatchers.IO)

        bleTestSuite = BLETestSuite(
            logger,
            coroutineScope
        )

        tangoSession = TangoSessionControllerImpl(
            logger
        )

        ///*
        bleUpgrade = BLEUpgradeControllerImpl(
            bleControllerFactory = { scopeBleUpgrade->
                BLEControllerImpl(
                    bleAdapterFactory = { scopeBle->
                        BLEAdapterImpl(
                            gattControllerFactory = { scopeAdapter, btDevice->
                                BLEGattControllerImpl(
                                    device = btDevice,
                                    context = applicationContext,
                                    logger = logger,
                                    coroutineScope = scopeAdapter
                                )
                            },
                            context = applicationContext,
                            logger = logger,
                            coroutineScope = scopeBle
                        )
                    },
                    bleScannerFactory = { bleAdapter->
                        BLEScannerImpl(
                            logger = logger,
                            adapter = bleAdapter as BLEAdapterImpl
                        )
                    },
                    logger = logger,
                    coroutineContext = scopeBleUpgrade.coroutineContext
                )
            },
            logger = logger,
            coroutineContext = coroutineScope.coroutineContext
        )

        val cryptographyController = CryptographyControllerImpl(
            context = applicationContext,
            logger = logger
        )

        tangoL1Controller = TangoL1ControllerImpl(
            tangoFirmwareApiFactory = {
                TangoFirmwareApiImpl(
                    logger = logger,
                    serviceFactory = {
                        createApiService(
                            urlBase = "https://api3.ful-mar.net",
                            certificate = ApiCertificateInput(
                                domain = "api3.ful-mar.net",
                                certificatePinSHA256 = "sha256/jU8P1uAp6g/xcQg/DeGxsi33poQjhMT9wmRFzR/AKsI="
                            ),
                            logger = logger,
                            serviceClass = TangoFirmwareApiService::class.java
                        )
                    }
                )
            },
            bleUpgrade,
            cryptographyController,
            tangoSession,
            TramaControllerImpl(),
            logger,
            coroutineScope,
        )
        //*/


        /*
        bleUpgrade = BLEUpgradeControllerImpl(
            bleControllerFactory = { scopeBleUpgrade->
                BLEControllerImpl(
                    bleAdapterFactory = { scopeBle->
                        BLEAdapterTestImpl(
                            name = BLETestK.TANGO_BLE_NAME,
                            gattControllerFactory = { scopeAdapter->
                                BLEGattControllerTestImpl(
                                    bleTestSuite = bleTestSuite,
                                    logger = logger,
                                    coroutineScope = scopeAdapter
                                )
                            },
                            logger = logger,
                            coroutineContext = scopeBle.coroutineContext
                        )
                    },
                    bleScannerFactory = { _->
                        BLEScannerTestImpl(
                            logger = logger,
                            device = BLEScannedDevice(
                                name = BLETestK.TANGO_BLE_NAME,
                                mac = "ABC123"
                            ),
                            coroutineScope = scopeBleUpgrade
                        )
                    },
                    logger = logger,
                    coroutineContext = scopeBleUpgrade.coroutineContext
                )
            },
            logger = logger,
            coroutineContext = coroutineScope.coroutineContext
        )

        val cryptographyController = CryptographyControllerTestImpl(
            applicationContext,
            logger = logger
        )


         */

    }
}