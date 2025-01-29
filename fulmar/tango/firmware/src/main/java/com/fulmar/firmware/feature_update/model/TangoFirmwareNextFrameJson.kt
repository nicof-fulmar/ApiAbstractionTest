package com.fulmar.firmware.feature_update.model

import com.google.gson.annotations.SerializedName

data class TangoFirmwareNextFrameJson (
    @SerializedName("DwFirmwareNextFrame")
    val data: TangoFirmwareNextFrameData
) {
    data class TangoFirmwareNextFrameData(
        val nextFrame: Int
    )
}