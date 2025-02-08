package com.fulmar.firmware.feature_api.utils

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response

object MockOkHttp3ResponseBuilder {
    operator fun invoke(): Response.Builder {
        return Response.Builder()
            .request(
                Request.Builder()
                .url("https://mocktest.com")
                .build()
            )
            .protocol(Protocol.HTTP_2)
            .message("OK")
    }
}