package com.fulmar.layer1.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

suspend fun tangoL1IncomingMessageProcessorService(
    messages: Flow<ByteArray>,
) {
    messages.collect { tMessage->

    }
}