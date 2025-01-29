package com.fulmar.api.model

data class ApiCertificateInput(
    val domain: String,
    val certificatePinSHA256: String
)
