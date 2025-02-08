package com.fulmar.tango.layer1

import android.content.Context
import com.fulmar.firmware.TangoFirmwareController
import com.fulmar.firmware.feature_api.TangoFirmwareApi
import com.fulmar.tango.layer1.TangoL1Controller
import com.fulmar.tango.layer1.config.TangoL1Config
import com.fulmar.tango.layer1.feature_incoming_message.TangoL1IncomingMessageProcessor
import com.fulmar.tango.layer1.models.TangoL1ReceiveFirmwareJSON
import com.fulmar.tango.layer1.models.TangoL1Status
import com.fulmar.tango.layer1.service.tangoL1SessionService
import com.fulmar.tango.session.TangoSessionController
import com.fulmar.tango.trama.controllers.TramaController
import com.fulmar.tango.trama.controllers.TramaControllerImpl
import com.fulmar.tango.trama.models.Commands
import com.fulmar.tango.trama.tramas.HeaderUI
import com.fulmar.tango.trama.tramas.TramaTx
import com.fulmar.tango.trama.tramas.toTrama
import com.google.gson.Gson
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.ble_upgrade.utils.characteristic
import com.supermegazinc.ble_upgrade.utils.message
import com.supermegazinc.ble_upgrade.utils.messageWithNotify
import com.supermegazinc.escentials.Status
import com.supermegazinc.escentials.firstWithTimeout
import com.supermegazinc.escentials.waitForNextWithTimeout
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TangoL1ControllerImpl(
    tangoFirmwareApiFactory: () -> TangoFirmwareApi,
    private val bleUpgradeController: BLEUpgradeController,
    private val cryptographyController: CryptographyController,
    private val tangoSessionController: TangoSessionController,
    private val tramaController: TramaController,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope,
): TangoL1Controller {

    private companion object {
        const val LOG_KEY = "TANGO-L1"
    }

    private val _status = MutableStateFlow<TangoL1Status>(TangoL1Status.Disconnected)
    override val status = _status.asStateFlow()

    private val sharedKeyFlow = tangoSessionController.session.map { (it as? Status.Ready)?.data?.sharedKey?.toByteArray() }
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            null
        )

    private val characteristicSendTelemetry = bleUpgradeController
        .characteristics
        .characteristic(TangoL1Config.CHARACTERISTIC_SEND_TELEMETRY)

    private val characteristicReceiveTelemetry = bleUpgradeController
        .characteristics
        .characteristic(TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY)

    private val characteristicSendFirmware = bleUpgradeController
        .characteristics
        .characteristic(TangoL1Config.CHARACTERISTIC_SEND_FIRMWARE)

    private val characteristicReceiveFirmware = bleUpgradeController
        .characteristics
        .characteristic(TangoL1Config.CHARACTERISTIC_RECEIVE_FIRMWARE)

    private val characteristicReceiveProgramacion = bleUpgradeController
        .characteristics
        .characteristic(TangoL1Config.CHARACTERISTIC_RECEIVE_PROGRAMACION)

    private val firmwareRaw = Channel<ByteArray>(capacity = 100, onBufferOverflow = BufferOverflow.SUSPEND)
    private val telemetryRaw = Channel<ByteArray>(Channel.CONFLATED)

    private val installedVersion = MutableSharedFlow<ByteArray>()

    private val incomingMessageProcessor = TangoL1IncomingMessageProcessor(
        firmwareRaw = firmwareRaw,
        telemetryRaw = telemetryRaw,
        onSendAck = {ack->
            coroutineScope.launch {
                characteristicSendTelemetry.firstOrNull()?.send(ack) ?: logger.e(LOG_KEY, "No se pudo enviar ack: No existe la caracteristica")
            }
        },
        tramaController = TramaControllerImpl(),
        cryptographyController = cryptographyController,
        sharedKey = sharedKeyFlow,
        logger = logger,
        coroutineScope = coroutineScope
    )

    private val firmwareController = TangoFirmwareController(
        connected = _status.map { it == TangoL1Status.Connected },
        onSendFirmwareInit = { message->
            val characteristic = characteristicSendFirmware.firstOrNull()?: run {
                logger.e(LOG_KEY, "No se pudo enviar FirmwareInit: No existe la caracteristica")
                return@TangoFirmwareController false
            }

            val shared = sharedKeyFlow.value ?: run {
                logger.e(LOG_KEY, "No se pudo enviar FirmwareInit: No existe la clave compartida")
                return@TangoFirmwareController false
            }

            val encrypted = cryptographyController.encrypt(message, shared) ?: run {
                logger.e(LOG_KEY, "No se pudo enviar FirmwareInit: No se pudo encriptar el mensaje")
                return@TangoFirmwareController false
            }

            characteristic.send(encrypted)
            return@TangoFirmwareController true
        },
        onSendFirmwareFrame = {message->
            val characteristic = characteristicSendFirmware.firstOrNull()?: run {
                logger.e(LOG_KEY, "No se pudo enviar FirmwareFrame: No existe la caracteristica")
                return@TangoFirmwareController false
            }
            characteristic.send(message)
            return@TangoFirmwareController true
        },
        firmwareRx = incomingMessageProcessor.firmware,
        logger = logger,
        coroutineScope = coroutineScope,
        onRequestTangoCurrentFirmwareVersion = {
            val characteristic = characteristicReceiveProgramacion.firstOrNull() ?: run {
                logger.e(LOG_KEY, "No se pudo solicitar la version instalada: No existe la caracteristica")
                return@TangoFirmwareController null
            }
            characteristic.forceRead()
            val encrypted = try {
                installedVersion.waitForNextWithTimeout(TangoL1Config.TANGO_FIRMWARE_VERSION_TIMEOUT)
            } catch (_: TimeoutCancellationException) {
                logger.e(LOG_KEY,"No se pudo solicitar la version instalada: Timeout")
                return@TangoFirmwareController null
            }

            val shared = sharedKeyFlow.value ?: run {
                logger.e(LOG_KEY, "No se pudo solicitar la version instalada: No existe la clave compartida")
                return@TangoFirmwareController null
            }

            val decrypted = cryptographyController.decrypt(encrypted, shared) ?: run {
                logger.e(LOG_KEY, "No se pudo solicitar la version instalada: No se pudo desencriptar")
                return@TangoFirmwareController null
            }

            val deserialized = try {
                Gson().fromJson(decrypted.decodeToString(), TangoL1ReceiveFirmwareJSON::class.java)!!
            } catch (_: Exception) {
                logger.e(LOG_KEY, "No se pudo solicitar la version instalada: No se pudo deserializar el JSON")
                return@TangoFirmwareController null
            }

            return@TangoFirmwareController deserialized.programacion.versionFw
            //return@TangoFirmwareController "1.0.1"

        },
        tangoFirmwareApiFactory = tangoFirmwareApiFactory
    )

    override val telemetry = incomingMessageProcessor.currentTelemetry

    private var newSessionJob: Job? = null
    private fun newSession() {
        newSessionJob?.cancel()
        newSessionJob = coroutineScope.launch {
            val result = run<Boolean> {
                try {
                    logger.i(LOG_KEY,"Generando sesion..")

                    val publicKey = tangoSessionController.refreshAndGetPublicKey().toByteArray()

                    val publicKeySigned = cryptographyController.sign(publicKey)

                    if(publicKeySigned == null) {
                        logger.e(LOG_KEY, "No se pudo firmar la clave publica")
                        return@run false
                    }

                    val publicKeySignedFull = publicKey + publicKeySigned

                    logger.d(LOG_KEY,"Buscando caracteristica 'ReceivePublicKey'..")
                    val receiveCharacteristic = bleUpgradeController
                        .characteristics
                        .map { it.firstOrNull {char-> TangoL1Config.CHARACTERISTIC_RECEIVE_KEY == char.uuid} }
                        .filterNotNull()
                        .firstWithTimeout(5000)
                    logger.d(LOG_KEY,"Encontrada, solicitando clave publica..")

                    receiveCharacteristic.forceRead()
                    val peerPublicKey = receiveCharacteristic.message
                        .receiveAsFlow().filterNotNull().firstWithTimeout(1000)
                    logger.d(LOG_KEY,"Encontrada, clave recibida [${peerPublicKey.size}]: ${peerPublicKey.toList()}")

                    if(!cryptographyController.verifyPublicKeySignature(peerPublicKey.copyOfRange(0,65),peerPublicKey.copyOfRange(65,65+256))) {
                        logger.e(LOG_KEY, "Verificacion de clave publica fallida")
                        return@run false
                    } else {
                        logger.d(LOG_KEY, "Verificacion de clave publica correcta")
                    }

                    logger.d(LOG_KEY,"Buscando caracteristica 'SendPublicKey'..")
                    val sendCharacteristic = bleUpgradeController
                        .characteristics
                        .mapNotNull { it.firstOrNull {char-> TangoL1Config.CHARACTERISTIC_SEND_KEY == char.uuid} }
                        .filterNotNull()
                        .firstWithTimeout(5000)
                    logger.d(LOG_KEY,"Encontrada, enviando clave publica [${publicKey.size}]: ${publicKey.toList()}")

                    sendCharacteristic.send(publicKeySignedFull)

                    logger.d(LOG_KEY,"Calculando clave compartida..")

                    val session = tangoSessionController.generateSession(peerPublicKey.copyOfRange(0,65).toList())

                    if(session==null) {
                        logger.e(LOG_KEY, "No se pudo calcular la clave compartida")
                        return@run false
                    }
                    logger.d(LOG_KEY,"Clave compartida calculada correctamente")

                    delay(1000)

                    logger.d(LOG_KEY,"Descubriendo servicios..")

                    val discoverServicesResult = withTimeoutOrNull(TangoL1Config.DISCOVER_SERVICES_TIMEOUT) {
                        bleUpgradeController.discoverServices()
                    }

                    if(discoverServicesResult==null) {
                        logger.e(LOG_KEY, "No se pudo descubrir los servicios: Timeout")
                        return@run false
                    } else if(!discoverServicesResult) {
                        logger.e(LOG_KEY, "No se pudo descubrir los servicios")
                        return@run false
                    }

                    logger.d(LOG_KEY,"Servicios descubiertos")

                    logger.i(
                        LOG_KEY,"Sesion generada con exito\n" +
                            "PUBLIC[${session.myPublicKey.size}]: ${session.myPublicKey}\n" +
                            "SHARED[${session.sharedKey.size}]: ${session.sharedKey}"
                    )

                    return@run true

                } catch (e: TimeoutCancellationException) {
                    logger.e(LOG_KEY, "Timeout")
                    return@run false
                }
            }

            if(result) {
                _status.emit(TangoL1Status.Connected)
            }
            else {
                logger.e(LOG_KEY, "No se pudo generar la sesion, abortando conexion")
                _status.emit(TangoL1Status.Disconnected)
                bleUpgradeController.disconnect()
            }
        }
    }

    override fun connect(name: String) {
        logger.i(LOG_KEY, "Intengo de conexion a '$name'")
        coroutineScope.launch {
            _status.update { TangoL1Status.Connecting }
            val result = bleUpgradeController.connect(
                name = name,
                servicesUUID = listOf(TangoL1Config.SERVICE_MAIN_UUID),
                mtu = 516
            )
            if(!result) {
                logger.e(LOG_KEY, "No se pudo conectar")
                _status.update { TangoL1Status.Disconnected }
            }
            //Ahora lo detecta el tangoL1SessionService y lanza una nueva sesion
        }
    }

    override fun disconnect() {
        logger.i(LOG_KEY, "Intengo de desconexion")
        newSessionJob?.cancel()
        bleUpgradeController.disconnect()
        _status.update { TangoL1Status.Disconnected }
    }

    private fun createPayload(payload: ByteArray, sharedKey: ByteArray): ByteArray? {
        val header = HeaderUI(cmd = Commands.RELAY)
        val headerBytes = tramaController.serialize(header.toTrama()!!)!!
        return cryptographyController.encrypt((headerBytes.toList() + payload.toList()).toByteArray(), sharedKey)
    }

    override suspend fun sendTelemetry(trama: TramaTx) {
        withContext(coroutineScope.coroutineContext) {
            logger.i(LOG_KEY, "Enviando mensaje: '${trama::class.simpleName}'\n${trama}")

            val shared = sharedKeyFlow.value ?: run {
                logger.e(LOG_KEY, "Error al obtener la clave compartida")
                return@withContext
            }

            val sendCharacteristic = bleUpgradeController.characteristics.firstOrNull()?.firstOrNull { it.uuid == TangoL1Config.CHARACTERISTIC_SEND_TELEMETRY } ?: run {
                logger.e(LOG_KEY, "Error al encontrar la caracteristica")
                return@withContext
            }

            val serialized = tramaController.serialize(trama)!!.toByteArray()
            val encrypted = createPayload(serialized, shared) ?: run {
                logger.e(LOG_KEY, "Error al encriptar")
                return@withContext
            }

            sendCharacteristic.send(encrypted)
            logger.d(LOG_KEY, "Mensaje enviado")
        }
    }

    init {

        coroutineScope.launch {
            tangoL1SessionService(
                bleUpgradeController.status,
                onNewSession = {
                    logger.d(LOG_KEY, "[SES-SERVICE] - onNewSession")
                    newSession()
                },
                onEndSession = {
                    logger.d(LOG_KEY, "[SES-SERVICE] - onEndSession")
                    newSessionJob?.cancel()
                }
            )
        }

        coroutineScope.launch {
            _status
                .collectLatest { status->
                    if(status==TangoL1Status.Connected) {
                        coroutineScope {
                            launch {
                                characteristicReceiveTelemetry
                                    .messageWithNotify()
                                    .collect {incomingMsg->
                                        telemetryRaw.send(incomingMsg)
                                    }
                            }
                            launch {
                                characteristicReceiveFirmware
                                    .messageWithNotify()
                                    .collect {incomingMsg->
                                        firmwareRaw.send(incomingMsg)
                                    }
                            }
                            launch {
                                characteristicReceiveProgramacion
                                    .message()
                                    .collect {incomingMsg->
                                        installedVersion.emit(incomingMsg)
                                    }
                            }
                        }
                    }
                }
        }

        coroutineScope.launch {
            bleUpgradeController
                .status
                .filter { it == BLEUpgradeConnectionStatus.Reconnecting }
                .collectLatest {_->
                    logger.i(LOG_KEY, "Reconexion detectada")
                    if(newSessionJob?.isActive==true) {
                        logger.d(LOG_KEY, "Deteniendo generacion de sesion")
                        newSessionJob?.cancel()
                    }
                    _status.update { TangoL1Status.Reconnecting }
                }
        }
    }
}