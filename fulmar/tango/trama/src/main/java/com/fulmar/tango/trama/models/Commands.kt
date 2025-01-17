package com.fulmar.tango.trama.models

enum class Commands(val code: Int) {
    NONE(0),
    ACK(1),
    RELAY(2),
    CACHED(3),
}