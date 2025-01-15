package com.fulmar.tango.trama.controllers

interface TramaController {
    fun serialize(target: Any): List<Byte>?
    fun deserialize(trama: List<Byte>, target: Any): Boolean
}