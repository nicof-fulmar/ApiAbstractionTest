package com.fulmar.api.di

import com.supermegazinc.logger.Logger

class ApiModuleInitializer(
    val logger: Logger
) {

    internal companion object {
        lateinit var _logger: Logger
    }

    init {
        _logger = logger
    }

}