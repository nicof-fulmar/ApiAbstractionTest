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
    logKey: String,
    logger: Logger,
) {
    connected.collectLatest { tConnected->
        if(!tConnected) return@collectLatest

        coroutineScope {
            try {
                logger.i(logKey, "Conexion detectada, chequeando versiones de firmware")
                val apiVersionAsync = async {
                    logger.d(logKey, "Solicitando ultima version..")
                    onRequestApiLatestFirmwareVersion()
                }
                val tangoVersionAsync = async {
                    logger.d(logKey, "Solicitando version instalada..")
                    onRequestTangoCurrentFirmwareVersion()
                }

                val apiVersion = apiVersionAsync.await()
                if(apiVersion==null) {
                    logger.e(logKey, "No se pudo obtener la ultima version")
                    return@coroutineScope
                }
                logger.d(logKey, "Ultima version: $apiVersion")

                val tangoVersion = tangoVersionAsync.await()
                if(tangoVersion==null) {
                    logger.e(logKey, "No se pudo obtener la version instalada")
                    return@coroutineScope
                }
                logger.d(logKey, "Version instalada: $tangoVersion")

                val compare = compareVersions(apiVersion, tangoVersion)

                if(compare < 0) {
                    logger.e(logKey, "[CRITIC] - La ultima version disponible es menor a la instalada")
                    return@coroutineScope
                } else if(compare==0) {
                    logger.d(logKey, "Ya esta instalada la ultima version")
                    return@coroutineScope
                } else {
                    logger.d(logKey, "Nueva version disponible, solicitando actualizacion de firmware")
                    onFirmwareUpdate(apiVersion)
                    return@coroutineScope
                }
            } catch (_: CancellationException) {
                logger.e(logKey, "Chequeo cancelado")
            }
        }
    }
}