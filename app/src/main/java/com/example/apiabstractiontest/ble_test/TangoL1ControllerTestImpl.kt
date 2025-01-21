package com.example.apiabstractiontest.ble_test

import android.content.Context
import com.example.apiabstractiontest.R
import com.fulmar.firmware.TangoFirmwareController
import com.fulmar.tango.layer1.TangoL1Controller
import com.fulmar.tango.layer1.config.TangoL1Config
import com.fulmar.tango.layer1.models.TangoL1Status
import com.fulmar.tango.layer1.models.TangoL1Telemetry
import com.fulmar.tango.layer1.service.tangoL1IncomingMessageProcessorService
import com.fulmar.tango.layer1.service.tangoL1SessionService
import com.fulmar.tango.session.TangoSessionController
import com.fulmar.tango.trama.controllers.TramaController
import com.fulmar.tango.trama.controllers.TramaControllerImpl
import com.fulmar.tango.trama.models.Commands
import com.fulmar.tango.trama.tramas.HeaderUI
import com.fulmar.tango.trama.tramas.ScabRx
import com.fulmar.tango.trama.tramas.SimpRx
import com.fulmar.tango.trama.tramas.TramaTx
import com.fulmar.tango.trama.tramas.toTicketUI
import com.fulmar.tango.trama.tramas.toTrama
import com.fulmar.tango.trama.tramas.toUI
import com.google.gson.Gson
import com.supermegazinc.ble_upgrade.BLEUpgradeController
import com.supermegazinc.ble_upgrade.model.BLEUpgradeConnectionStatus
import com.supermegazinc.escentials.Result
import com.supermegazinc.escentials.Status
import com.supermegazinc.escentials.firstWithTimeout
import com.supermegazinc.logger.Logger
import com.supermegazinc.security.cryptography.CryptographyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TangoL1ControllerTestImpl(
    private val bleUpgradeController: BLEUpgradeController,
    private val cryptographyController: CryptographyController,
    private val tangoSessionController: TangoSessionController,
    private val tramaController: TramaController,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope,
    context: Context
): TangoL1Controller {

    private companion object {
        const val LOG_KEY = "TANGO-L1"
    }

    private val incomingMessages = MutableSharedFlow<Pair<UUID, ByteArray>>()

    private val incomingFirmware = Channel<ByteArray>()
    private val firmwareTx = MutableSharedFlow<ByteArray>()

    private val _status = MutableStateFlow<TangoL1Status>(TangoL1Status.Disconnected)
    override val status = _status.asStateFlow()

    private val _telemetry = MutableStateFlow(TangoL1Telemetry())
    override val telemetry = _telemetry.asStateFlow()

    private val sharedKeyFlow = tangoSessionController.session.map { (it as? Status.Ready)?.data?.sharedKey?.toByteArray() }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    private val firmwareController = TangoFirmwareController(
        connected = _status.map{ it is TangoL1Status.Connected }.distinctUntilChanged(),
        onSendFirmwareInit = {
            firmwareTx.emit(it)
            true
        },
        onSendFirmwareFrame = {
            firmwareTx.emit(it)
            true
        },
        firmwareRx = incomingFirmware,
        onObtainFirmwareBinary = {
            context.resources.openRawResource(R.raw.firmware).readBytes()
        },
        logger = logger,
        coroutineScope = coroutineScope
    )

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
                        .map { it.firstOrNull {char-> TangoL1Config.CHARACTERISTIC_RECEIVE_KEY_UUID == char.uuid} }
                        .filterNotNull()
                        .firstWithTimeout(5000)
                    logger.d(LOG_KEY,"Encontrada, solicitando clave publica..")

                    receiveCharacteristic.forceRead()
                    val peerPublicKey = receiveCharacteristic.message
                        .filterNotNull()
                        .firstWithTimeout(5000)
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
                        .mapNotNull { it.firstOrNull {char-> TangoL1Config.CHARACTERISTIC_SEND_KEY_UUID == char.uuid} }
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

                    bleUpgradeController.discoverServices()
                    firmwareController.setTangoVersion("1.0.0")
                    firmwareController.setApiVersion("1.0.1")

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
                timeoutMillis = TangoL1Config.CONNECTION_TIMEOUT,
                servicesUUID = listOf(TangoL1Config.SERVICE_MAIN_UUID),
                mtu = 516
            )
            if(result is Result.Fail) {
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
        firmwareController.clear()
    }

    private fun createPayload(payload: ByteArray, sharedKey: ByteArray): ByteArray? {
        val header = HeaderUI(cmd = Commands.RELAY)
        val headerBytes = tramaController.serialize(header.toTrama()!!)!!
        return cryptographyController.encrypt((headerBytes.toList() + payload.toList()).toByteArray(), sharedKey)
    }

    override suspend fun sendTelemetry(payload: TramaTx) {
        withContext(coroutineScope.coroutineContext) {
            logger.i(LOG_KEY, "Enviando mensaje: '${payload::class.simpleName}'\n${payload}")

            val shared = sharedKeyFlow.value ?: run {
                logger.e(LOG_KEY, "Error al obtener la clave compartida")
                return@withContext
            }

            val sendCharacteristic = bleUpgradeController.characteristics.value.firstOrNull { it.uuid == TangoL1Config.CHARACTERISTIC_SEND_TELEMETRY_UUID } ?: run {
                logger.e(LOG_KEY, "Error al encontrar la caracteristica")
                return@withContext
            }

            val serialized = tramaController.serialize(payload)!!.toByteArray()
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
            tangoL1IncomingMessageProcessorService(
                messages = incomingMessages,
                tramaController = TramaControllerImpl(),
                cryptographyController = cryptographyController,
                sharedKey = sharedKeyFlow,
                logger = logger,
                onSendTelemetry = { message ->
                    logger.i(LOG_KEY, "Enviando: \n${message.toList()}")
                    launch {
                        bleUpgradeController.characteristics
                            .value
                            .firstOrNull { it.uuid == TangoL1Config.CHARACTERISTIC_SEND_TELEMETRY_UUID }
                            ?.send(message)
                    }
                },
                onReceiveTelemetry = { trama ->
                    logger.i(LOG_KEY, "Recibido: \n${trama}")

                    when (trama) {
                        is ScabRx -> {
                            _telemetry.update {
                                it.copy(scab = trama.toUI())
                            }
                        }

                        is SimpRx -> {
                            _telemetry.update {
                                it.copy(
                                    simpSummary = trama.toUI(),
                                    simpTicket = trama.toTicketUI()
                                )
                            }
                        }
                    }
                },
                onReceiveFirmware = {
                    coroutineScope.launch {
                        incomingFirmware.send(it)
                    }
                }
            )
        }

        coroutineScope.launch {
            coroutineScope {
                bleUpgradeController
                    .status
                    .filterIsInstance<BLEUpgradeConnectionStatus.Connected>()
                    .collectLatest { _->
                        coroutineScope {
                            launch {
                                bleUpgradeController
                                    .characteristics
                                    .mapNotNull { characteristics->
                                        characteristics.firstOrNull {
                                            it.uuid == TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID
                                        }
                                    }
                                    .distinctUntilChanged { old, new->
                                        old === new
                                    }
                                    .collectLatest { characteristic->

                                        characteristic.setNotification(true)

                                        characteristic
                                            .message
                                            .filterNotNull()
                                            .collect { incomingMsg->
                                                incomingMessages.emit(Pair(TangoL1Config.CHARACTERISTIC_RECEIVE_TELEMETRY_UUID, incomingMsg))
                                            }
                                    }
                            }
                            launch {
                                bleUpgradeController
                                    .characteristics
                                    .mapNotNull { characteristics->
                                        characteristics.firstOrNull {
                                            it.uuid == TangoL1Config.CHARACTERISTIC_RECEIVE_FIRMWARE
                                        }
                                    }
                                    .distinctUntilChanged { old, new->
                                        old === new
                                    }
                                    .collectLatest { characteristic->

                                        characteristic.setNotification(true)

                                        characteristic
                                            .message
                                            .filterNotNull()
                                            .collect { incomingMsg->
                                                incomingMessages.emit(Pair(TangoL1Config.CHARACTERISTIC_RECEIVE_FIRMWARE, incomingMsg))
                                            }
                                    }
                            }
                            /*
                            launch {
                                bleUpgradeController
                                    .characteristics
                                    .mapNotNull { characteristics->
                                        characteristics.firstOrNull {
                                            it.uuid == TangoL1Config.CHARACTERISTIC_RECEIVE_PROGRAMACION
                                        }
                                    }
                                    .distinctUntilChanged { old, new->
                                        old === new
                                    }
                                    .collectLatest { characteristic->

                                        characteristic.forceRead()

                                        characteristic
                                            .message
                                            .filterNotNull()
                                            .collect { incomingMsg->
                                                incomingMessages.emit(Pair(TangoL1Config.CHARACTERISTIC_RECEIVE_PROGRAMACION, incomingMsg))
                                            }
                                    }
                            }

                             */
                        }
                    }
            }
        }

        coroutineScope.launch {
            tangoFirmwareReceiverTest(
                incomingFirmware = firmwareTx,
                onSend = {
                    coroutineScope.launch {
                        incomingFirmware.send(it)
                    }
                },
                logger = logger,
                gson = Gson()
            )
        }

        coroutineScope.launch {
            bleUpgradeController
                .status
                .filterIsInstance<BLEUpgradeConnectionStatus.Reconnecting>()
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