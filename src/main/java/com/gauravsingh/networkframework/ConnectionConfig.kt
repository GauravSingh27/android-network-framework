package com.gauravsingh.networkframework

object ConnectionConfig {

    lateinit var baseUrl: String
        internal set

    lateinit var isNetworkConnected: () -> Boolean
        internal set

    var defaultHeaders: Headers? = null
        internal set

    var connectTimeOut: Int = 10000
        internal set

    var readTimeOut: Int = 10000
        internal set

    var predicate: (() -> Boolean)? = null
        internal set

    var beforeConnectionAction: (() -> Unit)? = null
        internal set

    var onSuccessfulConnectionAction: ((response: String) -> Unit)? = null
        internal set

    var onFailureConnectionAction: ((errorMessage: String, errorResponse: String?) -> Unit)? = null
        internal set
}

typealias Headers = HashMap<String, String>

fun ConnectionConfig.initialize(
    baseUrl: String,
    defaultHeaders: Headers,
    isNetworkConnected: () -> Boolean,
    connectTimeOut: Int = 10000,
    readTimeOut: Int = 10000,
    predicate: (() -> Boolean)? = null,
    beforeConnectionAction: (() -> Unit)? = null,
    onSuccessfulConnectionAction: ((response: String) -> Unit)? = null,
    onFailureConnectionAction: ((errorMessage: String, errorResponse: String?) -> Unit)? = null
) {
    this.baseUrl = baseUrl
    this.defaultHeaders = defaultHeaders
    this.isNetworkConnected = isNetworkConnected
    this.connectTimeOut = connectTimeOut
    this.readTimeOut = readTimeOut
    this.predicate = predicate
    this.beforeConnectionAction = beforeConnectionAction
    this.onSuccessfulConnectionAction = onSuccessfulConnectionAction
    this.onFailureConnectionAction = onFailureConnectionAction
}