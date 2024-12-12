package com.supermegazinc.escentials

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

suspend fun <T> Flow<T>.waitForNext(): T {
    return this.drop(1).first()
}

suspend inline fun <reified T> Flow<T>.waitForNextWithTimeout(timeMillis: Long): T {
    return withTimeout(timeMillis) {
        this@waitForNextWithTimeout
            .filterIsInstance<T>()
            .first()
    }
}

suspend inline fun <reified D> StateFlow<*>.waitForNextInstance(): D {
    return this.drop(1).filterIsInstance<D>().first()
}

suspend inline fun <reified D> StateFlow<*>.waitForNextInstanceWithTimeout(timeMillis: Long): D? {
    return withTimeoutOrNull(timeMillis) {
        this@waitForNextInstanceWithTimeout
            .drop(1)
            .filterIsInstance<D>()
            .first()
    }
}

suspend inline fun <reified D> SharedFlow<*>.waitForNextInstanceWithTimeout(timeMillis: Long): D? {
    return withTimeoutOrNull(timeMillis) {
        this@waitForNextInstanceWithTimeout
            .filterIsInstance<D>()
            .first()
    }
}