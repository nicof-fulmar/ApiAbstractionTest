package com.fulmar.api.model

sealed interface ApiResponseError {
    data object BadRequest : ApiResponseError
    data object BadResponse : ApiResponseError
    data object Timeout : ApiResponseError
    data object CantReach : ApiResponseError
    data object Unauthorized : ApiResponseError
    data class  Code(val code: Int) : ApiResponseError
}