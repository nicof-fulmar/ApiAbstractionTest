package com.fulmar.firmware

import com.fulmar.api.model.ApiResponseError
import com.fulmar.firmware.feature_api.TangoFirmwareApi
import com.fulmar.firmware.feature_api.model.CheckAndFetchInput
import com.fulmar.firmware.feature_api.model.CheckAndFetchOutput
import com.fulmar.firmware.feature_update.model.TangoFirmwareUpdateStatus
import com.fulmar.firmware.feature_update.util.tangoFirmwareUpdater
import com.fulmar.firmware.service.tangoFirmwareVersionService
import com.supermegazinc.escentials.Result
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TangoFirmwareController(
    private val connected: Flow<Boolean>,
    private val onRequestTangoCurrentFirmwareVersion: suspend () -> String?,
    private val onSendFirmwareInit: suspend (ByteArray) -> Boolean,
    private val onSendFirmwareFrame: suspend (ByteArray) -> Boolean,
    private val firmwareRx: ReceiveChannel<ByteArray>,
    tangoFirmwareApiFactory: () -> TangoFirmwareApi,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TANGO-FMW"
    }

    private val tangoFirmwareApi = tangoFirmwareApiFactory()

    private val _firmwareUpdateStatus = MutableStateFlow(TangoFirmwareUpdateStatus.NONE)
    val firmwareUpdateStatus = _firmwareUpdateStatus.asStateFlow()

    private suspend fun observeConnection() {
        connected.collect {
            if(!it && updateFirmwareJob?.isActive==true) {
                logger.e(LOG_KEY, "Desconexion detectada, cancelando actualizacion")
                clear()
            }
        }
    }

    private var updateFirmwareJob: Job? = null
    private fun taskUpdateFirmware(version: String, firmware: ByteArray) {
        updateFirmwareJob?.cancel()
        updateFirmwareJob = coroutineScope.launch {
            try {
                logger.i(LOG_KEY, "Inicio actualizacion de firmware")
                _firmwareUpdateStatus.update { TangoFirmwareUpdateStatus.UPDATING }
                val updateResult = tangoFirmwareUpdater(
                    version = version,
                    binary = firmware,
                    onSendFirmwareInit = {
                        onSendFirmwareInit(it)
                    },
                    firmwareRx = firmwareRx,
                    onSendFirmwareFrame = {
                        onSendFirmwareFrame(it)
                    },
                    logger = logger,
                    logKey = LOG_KEY
                )
                if(!updateResult) {
                    logger.e(LOG_KEY, "No se pudo actualizar el firmware")
                    return@launch
                } else {
                    logger.i(LOG_KEY, "Firmware actualizado con exito")
                    return@launch
                }
            } catch (e: CancellationException) {
                logger.e(LOG_KEY, "Actualizacion de firmware cancelada")
                return@launch
            } finally {
                _firmwareUpdateStatus.update { TangoFirmwareUpdateStatus.NONE }
            }
        }
    }

    init {
        coroutineScope.launch {
            tangoFirmwareVersionService(
                connected = connected,
                onRequestCheckAndFetch = { tangoVersion->
                    (tangoFirmwareApi.checkAndFetch(CheckAndFetchInput(tangoVersion)) as? Result.Success<CheckAndFetchOutput, ApiResponseError>)?.data
                },
                onRequestTangoCurrentFirmwareVersion = {
                    onRequestTangoCurrentFirmwareVersion()
                },
                onFirmwareUpdate = { version, firmware ->
                    taskUpdateFirmware(version, firmware)
                },
                logKey = LOG_KEY,
                logger = logger
            )
        }
        coroutineScope.launch {
            observeConnection()
        }
    }

    fun clear() {
        if(updateFirmwareJob?.isActive==true) {
            logger.e(LOG_KEY, "Cancelando actualizacion de firmware")
        }
        updateFirmwareJob?.cancel()
        _firmwareUpdateStatus.update { TangoFirmwareUpdateStatus.NONE }
    }
}