package com.fulmar.tango.layer1.models


import com.google.gson.annotations.SerializedName

data class TangoL1ReceiveFirmwareJSON(
    @SerializedName("Programacion")
    val programacion: Programacion
) {
    data class Programacion(
        @SerializedName("n_serie")
        val nSerie: String,
        @SerializedName("version_fw")
        val versionFw: String,
        @SerializedName("version_hw")
        val versionHw: String
    )
}