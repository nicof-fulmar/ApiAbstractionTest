package com.fulmar.firmware.feature_api.model


import com.google.gson.annotations.SerializedName

data class CheckAndFetchInput(
    @SerializedName("CurrentFirmwareVersion")
    val currentFirmwareVersion: String
)