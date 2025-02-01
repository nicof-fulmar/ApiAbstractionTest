package com.fulmar.api.utils

import com.fulmar.api.di.ApiModuleInitializer
import com.fulmar.api.model.ApiGenericResponse
import com.fulmar.api.model.ApiResponseError
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import com.supermegazinc.escentials.Result

suspend fun <T>makeApiCall(call: suspend () -> Response<T>): Result<T, ApiResponseError> {
    val logger = ApiModuleInitializer._logger
    val LOG_KEY = "API-CALL"

    var method = "NULL"
    var url = "NULL"
    return run<Result<T, ApiResponseError>> {
        try {
            val result = call.invoke()
            method = result.raw().request().method()
            url = result.raw().request().url().toString()
            logger.d(LOG_KEY, "[$method] - '$url'")

            if(result.isSuccessful) Result.Success(result.body()!!)
            else {
                result.errorBody()?.string()?.let {
                    try {
                        Gson().fromJson(it, ApiGenericResponse::class.java)?.let { tError->
                            Result.Fail(ApiResponseError.Code(tError.statusCode))
                        }
                    } catch (e: JsonSyntaxException) {
                        null
                    }
                } ?: Result.Fail(
                    when(val tCode = result.code()) {
                        500 -> ApiResponseError.BadResponse
                        400 -> ApiResponseError.BadRequest
                        401 -> ApiResponseError.Unauthorized
                        else -> ApiResponseError.Code(tCode)
                    }
                )
            }
        } catch (e: Exception) {
            Result.Fail(
                when(e) {
                    is NullPointerException -> ApiResponseError.BadResponse
                    is JsonSyntaxException -> ApiResponseError.BadResponse
                    is SocketTimeoutException -> ApiResponseError.Timeout
                    is IOException -> ApiResponseError.CantReach
                    else -> throw e
                }
            )
        }
    }.also { tResult->
        if(tResult is Result.Fail) {
            logger.e(LOG_KEY, "[$method] - '$url': ${tResult.error}")
        }
    }
}