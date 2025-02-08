package com.fulmar.firmware.service

import com.fulmar.firmware.feature_api.model.CheckAndFetchOutput
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

suspend fun tangoFirmwareVersionService(
    connected: Flow<Boolean>,
    onRequestTangoCurrentFirmwareVersion: suspend () -> String?,
    onRequestCheckAndFetch: suspend (version: String) -> CheckAndFetchOutput?,
    onFirmwareUpdate: (version: String, firmware: ByteArray) -> Unit,
    logKey: String,
    logger: Logger,
) {
    connected.collectLatest { tConnected->
        if(!tConnected) return@collectLatest

        coroutineScope {
            try {
                logger.i(logKey, "Conexion detectada, chequeando versiones de firmware")

                logger.d(logKey, "1. Solicitando version instalada..")
                val tangoVersion = onRequestTangoCurrentFirmwareVersion() ?: run {
                    logger.e(logKey, "No se pudo obtener la version instalada")
                    return@coroutineScope
                }
                logger.d(logKey, "Version instalada: $tangoVersion")

                logger.d(logKey, "2. Chequeando y descargando posible actualizacion..")
                val requestAndFetchResult = onRequestCheckAndFetch(tangoVersion) ?: run {
                    logger.e(logKey, "No se pudo realizar la solicitud")
                    return@coroutineScope
                }

                when(requestAndFetchResult) {
                    CheckAndFetchOutput.AlreadyUpdated -> {
                        logger.d(logKey, "Ya esta instalada la ultima version")
                        return@coroutineScope
                    }
                    is CheckAndFetchOutput.Firmware -> {
                        logger.d(logKey, "Nueva version descargada, solicitando actualizacion de firmware")
                        onFirmwareUpdate(requestAndFetchResult.version, requestAndFetchResult.firmware)
                        return@coroutineScope
                    }
                }
            } catch (_: CancellationException) {
                logger.e(logKey, "Chequeo cancelado")
            }
        }
    }
}