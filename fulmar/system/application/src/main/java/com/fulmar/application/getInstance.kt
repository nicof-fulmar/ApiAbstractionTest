package com.fulmar.application

import android.app.Application
import android.content.Context

inline fun <reified T> Context.getInstance(): T? {
    val app = applicationContext as Application
    return app::class.java.declaredFields
        .filter { it.type == T::class.java }
        .onEach { it.isAccessible = true }
        .firstNotNullOfOrNull { it.get(app) as? T }
}