package com.colisa.podplay.network

import retrofit2.Response

sealed class ApiResponse<T> {
    companion object {
        fun <T> create(error: Throwable): ApiErrorResponse<T> {
            return ApiErrorResponse(error.message ?: "Unknown error")
        }

        fun <T> create(response: Response<T>): ApiResponse<T> {
            return if (response.isSuccessful) {
                val body = response.body()
                if (body == null || response.code() == 204) {
                    ApiEmptyResponse()
                } else {
                    ApiSuccessResponse(body)
                }
            } else {
                val message = response.errorBody()?.toString()
                val errorMessage = if (message.isNullOrEmpty()) {
                    response.message()
                } else {
                    message
                }
                ApiErrorResponse(errorMessage ?: "Unknown error")
            }
        }
    }
}

data class ApiErrorResponse<T>(val message: String) : ApiResponse<T>()
class ApiEmptyResponse<T> : ApiResponse<T>()
data class ApiSuccessResponse<T>(val body: T) : ApiResponse<T>()