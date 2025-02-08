package com.fulmar.firmware.feature_api.model

sealed interface CheckAndFetchOutput {
    data class Firmware(val version: String, val firmware: ByteArray): CheckAndFetchOutput
    data object AlreadyUpdated: CheckAndFetchOutput
}