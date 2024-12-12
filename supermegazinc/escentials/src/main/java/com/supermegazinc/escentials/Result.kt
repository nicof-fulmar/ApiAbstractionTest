package com.supermegazinc.escentials

sealed interface Result<D, E> {
    data class Success<D, E>(val data: D) : Result<D, E>
    data class Fail<D, E>(val error: E, val data: D? = null) : Result<D, E>
}