package com.fulmar.firmware.service

import com.fulmar.firmware.utils.compareVersions
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

suspend fun tangoFirmwareVersionService(
    connected: Flow<Boolean>,
    onRequestApiLatestFirmwareVersion: suspend () -> String?,
    onRequestTangoCurrentFirmwareVersion: suspend () -> String?,
    onFirmwareUpdate: (version: String) -> Unit,
    LOG_KEY: String,
    logger: Logger,
) {
    connected.collectLatest { tConnected->
        if(!tConnected) return@collectLatest

        coroutineScope {
            try {
                logger.i(LOG_KEY, "Conexion detectada, chequeando versiones de firmware")
                val apiVersionAsync = async {
                    logger.d(LOG_KEY, "Solicitando ultima version..")
                    onRequestApiLatestFirmwareVersion()
                }
                val tangoVersionAsync = async {
                    logger.d(LOG_KEY, "Solicitando version instalada..")
                    onRequestTangoCurrentFirmwareVersion()
                }

                val apiVersion = apiVersionAsync.await()
                if(apiVersion==null) {
                    logger.e(LOG_KEY, "No se pudo obtener la ultima version")
                    return@coroutineScope
                }
                logger.d(LOG_KEY, "Ultima version: $apiVersion")

                val tangoVersion = tangoVersionAsync.await()
                if(tangoVersion==null) {
                    logger.e(LOG_KEY, "No se pudo obtener la version instalada")
                    return@coroutineScope
                }
                logger.d(LOG_KEY, "Version instalada: $tangoVersion")

                val compare = compareVersions(apiVersion, tangoVersion)

                if(compare < 0) {
                    logger.e(LOG_KEY, "[CRITIC] - La ultima version disponible es menor a la instalada")
                    return@coroutineScope
                } else if(compare==0) {
                    logger.d(LOG_KEY, "Ya esta instalada la ultima version")
                    return@coroutineScope
                } else {
                    logger.d(LOG_KEY, "Nueva version disponible, solicitando actualizacion de firmware")
                    onFirmwareUpdate(apiVersion)
                    return@coroutineScope
                }
            } catch (_: CancellationException) {
                logger.e(LOG_KEY, "Chequeo cancelado")
            }
        }
    }
}