package com.fulmar.firmware

import com.supermegazinc.logger.Logger

class TestLogger : Logger {
    override fun d(tag: String?, message: String) = println("D[$tag] - $message")
    override fun e(tag: String?, message: String) = println("E[$tag] - $message")
    override fun i(tag: String?, message: String) = println("I[$tag] - $message")
}