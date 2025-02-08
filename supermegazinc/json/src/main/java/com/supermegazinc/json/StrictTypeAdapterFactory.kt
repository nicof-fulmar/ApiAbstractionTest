package com.supermegazinc.json

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class StrictTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        @Suppress("UNCHECKED_CAST")
        val clazz = type.rawType as Class<T>
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                val gsonn = Gson()
                gsonn.toJson(gsonn.toJsonTree(value), out)
            }

            override fun read(reader: JsonReader): T {
                val jsonElement = JsonParser().parse(reader)
                if (!jsonElement.isJsonObject) {
                    throw JsonParseException("Expected a JSON object")
                }

                val jsonObject = jsonElement.asJsonObject
                val expectedFields = clazz.declaredFields.map { field ->
                    field.getAnnotation(SerializedName::class.java)?.value ?: field.name
                }.toSet()
                val actualFields = jsonObject.keySet()

                if (actualFields != expectedFields) {
                    throw JsonParseException("JSON does not match the expected structure for class ${clazz.simpleName}")
                }

                for (field in expectedFields) {
                    if (!jsonObject.has(field)) {
                        throw JsonParseException("Missing field: $field in class ${clazz.simpleName}")
                    }
                }

                return Gson().fromJson(jsonElement, clazz) as T
            }
        }.nullSafe() as TypeAdapter<T>
    }
}