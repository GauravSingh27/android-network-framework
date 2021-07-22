package com.gauravsingh.networkframework

enum class ErrorCode(val code: Int, val message: String) {
    NO_INTERNET(999, "No internet"),
    PREDICATE_NOT_SATISFIED(998, "Predicate not satisfied")
}