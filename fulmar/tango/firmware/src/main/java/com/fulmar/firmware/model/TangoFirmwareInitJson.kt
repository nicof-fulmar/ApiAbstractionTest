package com.fulmar.firmware.model

import com.google.gson.annotations.SerializedName

data class TangoFirmwareInitJson(
    @SerializedName("InitDwFirmware")
    val data: TangoFirmwareInitData
) {
    data class TangoFirmwareInitData(
        val version: String,
        val size: Int,
        @SerializedName("cantPaquetes")
        val packetQty: Int
    )
}