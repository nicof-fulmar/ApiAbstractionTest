package com.fulmar.firmware.utils

fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.split(".").map { it.toInt() }
    val parts2 = v2.split(".").map { it.toInt() }

    for (i in 0 until maxOf(parts1.size, parts2.size)) {
        val num1 = parts1.getOrElse(i) { 0 }
        val num2 = parts2.getOrElse(i) { 0 }

        if (num1 != num2) {
            return num1.compareTo(num2)
        }
    }
    return 0
}