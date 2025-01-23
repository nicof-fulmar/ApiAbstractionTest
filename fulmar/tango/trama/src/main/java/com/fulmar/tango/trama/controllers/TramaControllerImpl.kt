package com.fulmar.tango.trama.controllers

import com.supermegazinc.logger.Logger
import com.supermegazinc.logger.LoggerCustom
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * TramaController es un conversor bidireccional entre listas de bytes (tramas)
 * y objectos, para serializar y deserializar rápidamente con un debug conciso.
 * El módulo hace uso de Reflect para acceder a las propiedades de las clases.
 * @author Nicolás Ferreyra
 */
@Suppress("UNCHECKED_CAST", "UNUSED_EXPRESSION")
class TramaControllerImpl(
    private val logger: Logger = LoggerCustom(false, false, false)
): TramaController {

    private companion object {
        const val ENABLE_DEBUG = true
        const val GLOBAL_LOG_KEY = "trama"
        const val DESERIALIZE_LOG_KEY = "des"
        const val SERIALIZE_LOG_KEY = "ser"

        const val SERIALIZE_REQ = "Intento de serializar: "
        const val SERIALIZE_START = "Serializando: "
        const val SERIALIZE_RESULT = "Serializado: "

        const val DESERIALIZE_REQ = "Intento de deserializar: "
        const val DESERIALIZE_START = "Deserializando:  "
        const val DESERIALIZE_VALIDATE_ERROR = "Error de validación: "
        const val DESERIALIZE_RESULT = "Deserializado: "


        const val OBJECT_NOT_DATA = ": El objeto no es data class"
        const val OBJECT_EMPTY = ": El objeto no tiene elementos"
        const val CANT_MATCH = "No se encontró coincidencia para: "
        const val INVALID_TYPE = "El tipo del elemento es inválido: "
        const val OVERFLOW = "Largo excedido: "
    }

    /**
     * Convierte cualquier *data class* que contenga  [Byte] , [List]<[Byte]>, o [MutableList]<[Byte]> en un [ByteArray].
     *
     *@param target El *data class* que contiene los datos cargados.
     *@return Un [ByteArray] con todos los bytes de [target] ordenados, o
     *null si [target] no es *data class*, o si contiene un tipo inválido.
     */
    override fun serialize(target: Any): List<Byte>? {

        if(!target::class.isData) {
            if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY", "${target::class.simpleName}$OBJECT_NOT_DATA")
            return null
        }

        val properties = target::class.memberProperties.also {tProperties->
            if(tProperties.isEmpty()) {
                if(ENABLE_DEBUG) logger.d("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY", "$SERIALIZE_REQ${target::class.simpleName}")
                if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY", "${target::class.simpleName}$OBJECT_EMPTY")
                return null
            }
        }

        target::class.primaryConstructor?.parameters.also {tParameters->
            if(tParameters.isNullOrEmpty()) {
                if(ENABLE_DEBUG) logger.d("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY", "$SERIALIZE_REQ${target::class.simpleName}")
                if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY", "${target::class.simpleName}$OBJECT_EMPTY")
                return null
            }
        }?.map {tParameter->
            properties.firstOrNull{it.name == tParameter.name}.let {tProperty->
                if(tProperty == null) {
                    if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY", "$CANT_MATCH${tParameter.name}")
                    return null
                }
                val name = tProperty.name
                val type = tProperty.returnType.toString()
                val value = tProperty.getter.call(target)
                when(type) {
                    "kotlin.Byte" -> {
                        listOf(value as Byte).toByteArray()
                    }
                    "kotlin.collections.List<kotlin.Byte>" -> {
                        (value as List<Byte>).toByteArray()
                    }
                    "kotlin.collections.MutableList<kotlin.Byte>" -> {
                        (value as MutableList<Byte>).toByteArray()
                    }
                    else -> null
                }.let {
                    if(it==null) {
                        if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY", "$INVALID_TYPE${target::class.simpleName} - $name: $type")
                        return null
                    }
                    Pair(name, it)
                }
            }
        }?.also {tValues->

            if(ENABLE_DEBUG) logger.d("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY", "$SERIALIZE_START${target::class.simpleName}\n${
                tValues.joinToString("\n") {"${it.first}: \"${it.second.decodeToString()}\" ; [${it.second.size}](${it.second.joinToString(", ") {byte-> byte.toString()}})"}
            }")

            ArrayList<Byte>().apply {
                tValues.map{ttValues-> ttValues.second.toList()}.let {tArray->
                    tArray.forEach{tList-> addAll(tList)}
                }
            }.also {tOut->
                if(ENABLE_DEBUG) logger.i("$GLOBAL_LOG_KEY-$SERIALIZE_LOG_KEY",
                    "$SERIALIZE_RESULT${target::class.simpleName}\n" +
                            "Text: ${tOut.toByteArray().decodeToString()}\n" +
                            "Bytes[${tOut.size}]: [${tOut.joinToString(", ") {it.toString()}}]")
                return tOut
            }
        }
        return null
    }

    /**
     * Pasa cualquier [List]<[Byte]> a cualquier *data class* que contenga  [Byte] , [List]<[Byte]>, o [MutableList]<[Byte]>.
     * Esta función permite, además de transferir los datos, validarlos. El *data class* se construye con las siguientes variantes:
     * - var out: [Byte] -> Se pasa el byte de la trama a out.
     * - val in: [Byte] = 'H' -> Se valida si el byte de la trama es in ('H').
     * - val out: [MutableList]<[Byte]> = [MutableList](4){0} -> Se pasan 4 bytes de la trama a out.
     * - val in: [List]<[Byte]> = "hola".toByteArray().toList() -> Se valida si los 4 bytes de la trama son in ('h','o','l','a').
     *@param target El *data class* al que se quieren pasar los datos.
     *@return `true` si la transferencia fue exitosa o `false` si [target] no es *data class*, si no supera
     * alguna de las validaciones, o si alguno de los tipos son invalidos.
     */
    override fun deserialize(trama: List<Byte>, target: Any): Boolean {

        if(!target::class.isData) {
            if(ENABLE_DEBUG) logger.d("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "$DESERIALIZE_REQ${target::class.simpleName}")
            if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "${target::class.simpleName}$OBJECT_NOT_DATA")
            return false
        }

        val properties = target::class.memberProperties.also {tProperties->
            if(tProperties.isEmpty()) {
                if(ENABLE_DEBUG) logger.d("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "$DESERIALIZE_REQ${target::class.simpleName}")
                if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "${target::class.simpleName}$OBJECT_EMPTY")
                return false
            }
        }

        if(ENABLE_DEBUG) logger.d("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY",
            "$DESERIALIZE_START${target::class.simpleName}\n" +
                    "Text: ${trama.toByteArray().decodeToString()}\n" +
                    "Bytes[${trama.size}]: [${trama.joinToString(", ") {it.toString()}}]")


        var index = 0
        target::class.primaryConstructor?.parameters.also {tParameters->
            if(tParameters.isNullOrEmpty()) {
                if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "${target::class.simpleName}$OBJECT_EMPTY")
                return false
            }
        }?.map {tParameter->
            properties.firstOrNull{it.name == tParameter.name}.also {tProperty->
                if(tProperty == null) {
                    if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "$CANT_MATCH${tParameter.name}")
                    return false
                }
            }?.apply {
                try { this as KMutableProperty1 }
                catch (_: Exception) { this }
            }!!.let {tProperty->
                val name = tProperty.name
                try {
                    when(val type = tProperty.returnType.toString()) {
                        "kotlin.Byte" -> {
                            if(tProperty is KMutableProperty1) {
                                trama[index.also{index++}].also {
                                    tProperty.setter.call(target, it)
                                }.let { listOf(it) }
                            } else {
                                trama[index.also{index++}].also {
                                    if((tProperty.getter.call(target) as Byte) != it) {
                                        if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "$DESERIALIZE_VALIDATE_ERROR${target::class.simpleName} -> ${tProperty.name}")
                                        return false
                                    }
                                }.let { listOf(it) }
                            }
                        }
                        "kotlin.collections.List<kotlin.Byte>" -> {
                            (tProperty.getter.call(target) as List<Byte>).onEach {
                                if(trama[index.also{index++}] != it) {
                                    if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "$DESERIALIZE_VALIDATE_ERROR${target::class.simpleName} -> ${tProperty.name}")
                                    return false
                                }
                            }
                        }
                        "kotlin.collections.MutableList<kotlin.Byte>" -> {
                            (tProperty.getter.call(target) as MutableList<Byte>).apply {
                                forEachIndexed { tIndex, _ ->
                                    this[tIndex] = trama[index.also{index++}]
                                }
                            }
                        }
                        else -> {
                            if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "$INVALID_TYPE${target::class.simpleName} - $name: $type")
                            return false
                        }
                    }
                } catch (e: IndexOutOfBoundsException) {
                    if(ENABLE_DEBUG) logger.e("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "$OVERFLOW${target::class.simpleName}")
                    return false
                }.let {
                    Pair(name, it.toByteArray())
                }
            }
        }?.also {tValues->
            if(ENABLE_DEBUG) logger.i("$GLOBAL_LOG_KEY-$DESERIALIZE_LOG_KEY", "$DESERIALIZE_RESULT${target::class.simpleName}\n${
                tValues.joinToString("\n") {"${it.first}: \"${it.second.decodeToString()}\" ; [${it.second.size}](${it.second.joinToString(", ") {byte-> byte.toString()}})"}
            }")
            return true
        }
        return false
    }
}