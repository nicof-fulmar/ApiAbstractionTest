package com.fulmar.firmware

import com.fulmar.firmware.feature_api.TangoFirmwareApi
import com.fulmar.firmware.feature_update.config.TangoFirmwareConfig
import com.fulmar.firmware.feature_update.model.TangoFirmwareInitJson
import com.fulmar.firmware.feature_update.model.TangoFirmwareUpdateStatus
import com.fulmar.firmware.feature_update.util.dividePacket
import com.fulmar.firmware.feature_update.util.tangoFirmwareSender
import com.fulmar.firmware.service.tangoFirmwareVersionService
import com.google.gson.Gson
import com.supermegazinc.escentials.Result
import com.supermegazinc.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TangoFirmwareController(
    private val connected: Flow<Boolean>,
    private val onRequestTangoCurrentFirmwareVersion: suspend () -> String?,
    private val onSendFirmwareInit: suspend (ByteArray) -> Boolean,
    private val onSendFirmwareFrame: suspend (ByteArray) -> Boolean,
    private val firmwareRx: ReceiveChannel<ByteArray>,
    private val tangoFirmwareApi: TangoFirmwareApi,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TANGO-FMW"
    }

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
    private fun taskUpdateFirmware(apiVersion: String) {
        updateFirmwareJob?.cancel()
        updateFirmwareJob = coroutineScope.launch {
            try {
                run {
                    _firmwareUpdateStatus.update { TangoFirmwareUpdateStatus.UPDATING }
                    logger.i(LOG_KEY, "Inicio actualizacion de firmware")
                    logger.d(LOG_KEY, "1. Obtener el binario del firmware")
                    val binary = (tangoFirmwareApi.fetchFile() as? Result.Success)?.data
                    if(binary==null || binary.isEmpty()) {
                        logger.e(LOG_KEY, "Error al obtener el binario")
                        return@run
                    }
                    val binarySize = binary.size
                    logger.d(LOG_KEY, "Binario obtenido con exito. Tamaño: $binarySize bytes")
                    logger.d(LOG_KEY, "2. Dividir el binario en paquetes de ${TangoFirmwareConfig.PACKET_SIZE_BYTES} bytes")
                    val dividedFirmware = binary.dividePacket(TangoFirmwareConfig.PACKET_SIZE_BYTES)
                    val packetQty =  dividedFirmware.size
                    logger.d(LOG_KEY, "Binario dividido en $packetQty partes")

                    logger.d(LOG_KEY, "3. Enviar solicitud de actualizacion de firmware")
                    val firmwareInit = TangoFirmwareInitJson.TangoFirmwareInitData(
                        version = apiVersion,
                        size = binarySize,
                        packetQty = packetQty
                    )
                    val gson = Gson()
                    val serializedFirmwareInit = gson.toJson(TangoFirmwareInitJson(firmwareInit)).toByteArray()
                    if(!onSendFirmwareInit(serializedFirmwareInit)) {
                        logger.e(LOG_KEY, "Error al enviar la solicitud de actualizacion de firmware")
                        return@run
                    }
                    logger.d(LOG_KEY, "Solicitud de actualizacion de firmware enviada")

                    logger.d(LOG_KEY, "4. Espero peticion de NextFrame y comienzo el envio del firmware")

                    if(!tangoFirmwareSender(
                            onSendFirmwareFrame = { onSendFirmwareFrame(it) },
                            firmwareRx = firmwareRx,
                            binary = dividedFirmware,
                            receiveTimeoutMs = TangoFirmwareConfig.RECEIVE_TIMEOUT_MS,
                            gson = gson,
                            logger = logger,
                            LOG_KEY = LOG_KEY
                        )
                    ) {
                        logger.e(LOG_KEY, "No se pudo enviar el firmware")
                        return@run
                    }
                    logger.i(LOG_KEY, "Firmware enviado con exito")
                    return@run
                }
                delay(2000)
                _firmwareUpdateStatus.update { TangoFirmwareUpdateStatus.NONE }
            } catch (e: CancellationException) {
                logger.e(LOG_KEY, "Actualizacion de firmware cancelada")
            }
        }
    }

    init {
        coroutineScope.launch {
            tangoFirmwareVersionService(
                connected = connected,
                onRequestApiLatestFirmwareVersion = {
                    (tangoFirmwareApi.getLatestFirmwareVersion() as? Result.Success)?.data?.actualFirmware
                },
                onRequestTangoCurrentFirmwareVersion = {
                    onRequestTangoCurrentFirmwareVersion()
                },
                onFirmwareUpdate = { version->
                    taskUpdateFirmware(version)
                },
                LOG_KEY = LOG_KEY,
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