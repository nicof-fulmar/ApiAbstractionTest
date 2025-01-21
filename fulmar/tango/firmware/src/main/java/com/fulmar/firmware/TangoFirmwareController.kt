package com.fulmar.firmware

import com.fulmar.firmware.config.TangoFirmwareConfig
import com.fulmar.firmware.model.TangoFirmwareInitJson
import com.fulmar.firmware.model.TangoFirmwareUpdateStatus
import com.fulmar.firmware.util.dividePacket
import com.fulmar.firmware.util.tangoFirmwareSender
import com.google.gson.Gson
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
    private val onSendFirmwareInit: suspend (ByteArray) -> Boolean,
    private val onSendFirmwareFrame: suspend (ByteArray) -> Boolean,
    private val firmwareRx: ReceiveChannel<ByteArray>,
    private val onObtainFirmwareBinary: suspend () -> ByteArray?,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope
) {

    private companion object {
        const val LOG_KEY = "TANGO-FMW"
    }

    private val _firmwareUpdateStatus = MutableStateFlow(TangoFirmwareUpdateStatus.NONE)
    val firmwareUpdateStatus = _firmwareUpdateStatus.asStateFlow()

    private val _apiVersion = MutableStateFlow<String?>(null)
    private val _tangoVersion = MutableStateFlow<String?>(null)

    fun setApiVersion(version: String) {
        logger.i(LOG_KEY, "Seteando version de firmware [API]: '$version'")
        _apiVersion.update { version }
    }

    fun setTangoVersion(version: String) {
        logger.i(LOG_KEY, "Seteando version de firmware [TANGO]: '$version'")
        _tangoVersion.update { version }
    }

    private suspend fun observeVersions() {
        combine(_apiVersion, _tangoVersion, _firmwareUpdateStatus, connected) { apiV, tangoV, updStatus, connected ->
            if(
                apiV!=null &&
                tangoV!=null &&
                updStatus==TangoFirmwareUpdateStatus.NONE &&
                apiV!=tangoV &&
                connected
            ) {
                logger.i(LOG_KEY, "Se detecto una version de firmware ($apiV) distinta a la instalada ($tangoV). Iniciando procedimiento de actualizacion de firmware..")
                taskUpdateFirmware(apiV)
            }
        }.collect()
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
                    val binary = onObtainFirmwareBinary.invoke()
                    if(binary==null|| binary.isEmpty()) {
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
                        )) {
                        logger.e(LOG_KEY, "No se pudo enviar el firmware")
                        return@run
                    }
                    logger.i(LOG_KEY, "Firmware enviado con exito")
                    _tangoVersion.emit(_apiVersion.value)
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
            observeVersions()
        }
    }

    fun clear() {
        updateFirmwareJob?.cancel()
        _firmwareUpdateStatus.update { TangoFirmwareUpdateStatus.NONE }
    }
}