package com.colisa.podplay.network

sealed class Result<out R> {
    data class OK<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}