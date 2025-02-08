package com.fulmar.firmware

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fulmar.api.di.ApiModuleInitializer
import com.fulmar.firmware.feature_api.TangoFirmwareApiImpl
import com.fulmar.firmware.feature_update.model.TangoFirmwareInitJson
import com.fulmar.firmware.feature_update.model.TangoFirmwareNextFrameJson
import com.fulmar.firmware.utils.FirmwareApiServiceTestCase
import com.fulmar.firmware.utils.createMockFirmwareApiService
import com.supermegazinc.json.strictGson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

enum class Procedure {
    CONNECTED_FALSE,
    CONNECTED_TRUE,
    CURRENT_FIRMWARE_REQUESTED,
    FIRMWARE_INIT_REQUESTED,
    FIRMWARE_UPDATE_COMPLETE
}

fun Procedure.next(): Procedure? {
    return try {
        Procedure.entries[this.ordinal+1].also {
            TestLogger().i("TEST", "Paso ${it.ordinal}: ${it.name}")
        }
    } catch (_: Exception) {
        null
    }
}


@RunWith(JUnit4::class)
class TangoFirmwareControllerTest {

    @Before
    fun setup() {
        ApiModuleInitializer(TestLogger())
    }

    @Test
    fun testTangoFirmwareController(): Unit = runBlocking {

        var step: Procedure = Procedure.CONNECTED_FALSE

        val connected = MutableStateFlow(false)

        val testResult = MutableSharedFlow<Boolean>()

        val onSendFirmwareInit = MutableSharedFlow<ByteArray>()
        val sendFirmwareFrameChannel = Channel<ByteArray>(capacity = 100, onBufferOverflow = BufferOverflow.SUSPEND)
        val receiveFirmwareChannel = Channel<ByteArray>(capacity = 100, onBufferOverflow = BufferOverflow.SUSPEND)

        val jobs: ArrayList<kotlinx.coroutines.Job> = arrayListOf()

        jobs.add(launch {
            onSendFirmwareInit.collectLatest { payload->
                coroutineScope {
                    if(step!=Procedure.FIRMWARE_INIT_REQUESTED) {
                        testResult.emit(false)
                    } else {
                        val deserialized = try{
                            strictGson().fromJson(payload.decodeToString(), TangoFirmwareInitJson::class.java)
                        } catch(_: Exception) {
                            testResult.emit(false)
                            return@coroutineScope
                        }

                        val packetQty = deserialized.data.packetQty
                        var lastReceived = 1

                        while(isActive && lastReceived < packetQty+1) {
                            val nextFrame = TangoFirmwareNextFrameJson(
                                TangoFirmwareNextFrameJson.TangoFirmwareNextFrameData(lastReceived)
                            )
                            val nextFrameSerialized = strictGson().toJson(nextFrame)
                            receiveFirmwareChannel.send(nextFrameSerialized.toByteArray())
                            sendFirmwareFrameChannel.receive()
                            lastReceived++
                        }
                        if(lastReceived==packetQty+1) {
                            step = step.next()!!
                            testResult.emit(true)
                        } else {
                            testResult.emit(false)
                        }
                    }
                }
            }
        })

        jobs.add(launch {
            delay(1000)
            if(step==Procedure.CONNECTED_FALSE) {
                step = step.next()!!
                connected.emit(true)
            } else {
                testResult.emit(false)
            }
        })

        TangoFirmwareController(
            connected = connected,
            onRequestTangoCurrentFirmwareVersion = {
                if(step!=Procedure.CONNECTED_TRUE) {
                    testResult.emit(false)
                    null
                }
                else {
                    step = step.next()!!
                    "A.1.0.5"
                }
            },
            onSendFirmwareInit = {
                if(step!=Procedure.CURRENT_FIRMWARE_REQUESTED) {
                    testResult.emit(false)
                    false
                }
                else {
                    step = step.next()!!
                    onSendFirmwareInit.emit(it)
                    true
                }
            },
            onSendFirmwareFrame = { frame->
                if(step!=Procedure.FIRMWARE_INIT_REQUESTED) {
                    testResult.emit(false)
                    false
                } else {
                    sendFirmwareFrameChannel.trySend(frame)
                    true
                }
            },
            firmwareRx = receiveFirmwareChannel,
            tangoFirmwareApiFactory = {
                TangoFirmwareApiImpl(
                    logger = TestLogger(),
                    serviceFactory = {
                        createMockFirmwareApiService(FirmwareApiServiceTestCase.NEW_VERSION_AVAILABLE)
                    }
                )
            },
            logger = TestLogger(),
            coroutineScope = CoroutineScope(Dispatchers.Default)
        )

        launch {
            delay(10000)
            testResult.emit(false)
        }

        val result = testResult.first()
        jobs.forEach { job->
            job.cancel()
        }
        assert(result)
    }
}