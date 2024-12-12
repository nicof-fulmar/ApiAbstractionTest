package com.supermegazinc.escentials

fun calculateTimeMs(from: Long): Long {
    val current = System.currentTimeMillis()
    if(current<from) throw IllegalArgumentException("Really?")
    return System.currentTimeMillis() - from
}