package com.gauravsingh.networkframework

sealed class Result<T>(val responseCode: Int)

class Success<T>(responseCode: Int, val response: T) : Result<T>(responseCode)

class Error<T>(responseCode: Int, val errorMessage: String?, val errorResponse: T?) : Result<T>(responseCode)