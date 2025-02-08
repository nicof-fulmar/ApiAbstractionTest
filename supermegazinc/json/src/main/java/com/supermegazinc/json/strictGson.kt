package com.supermegazinc.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder

fun strictGson(): Gson {
    return GsonBuilder()
        .registerTypeAdapterFactory(StrictTypeAdapterFactory())
        .create()
}