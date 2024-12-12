package com.supermegazinc.escentials

sealed interface Status<T> {
    class None<T>: Status<T>
    class Loading<T>: Status<T>
    data class Ready<T>(val data: T) : Status<T>
}