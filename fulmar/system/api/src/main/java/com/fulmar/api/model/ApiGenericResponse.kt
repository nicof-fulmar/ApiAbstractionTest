package com.fulmar.api.model

data class ApiGenericResponse<T> (
    val message: String,
    val statusCode: Int,
    val data: T
)