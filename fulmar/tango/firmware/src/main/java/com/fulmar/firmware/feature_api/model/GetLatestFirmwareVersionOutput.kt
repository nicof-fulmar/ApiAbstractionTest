package com.fulmar.firmware.feature_api.model


import com.google.gson.annotations.SerializedName

data class GetLatestFirmwareVersionOutput(
    @SerializedName("actualFirmware")
    val actualFirmware: String
)