package com.fulmar.firmware

import android.app.Application
import com.fulmar.api.di.ApiModuleInitializer
import com.supermegazinc.logger.Logger

class AppTest: Application() {

    override fun onCreate() {
        super.onCreate()
        val logger: Logger = TestLogger()
        ApiModuleInitializer(logger)
    }

}