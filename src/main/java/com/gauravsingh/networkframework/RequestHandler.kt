package com.gauravsingh.networkframework

import android.net.Uri
import com.gauravsingh.networkframework.ConnectionConfig.isNetworkConnected
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection


inline fun <reified T> Request.execute(
    parser: (String) -> T,
    connectionProvider: (url: String) -> HttpURLConnection = ::getConnection,
    noinline predicate: (() -> Boolean)? =
        ConnectionConfig.predicate,
    noinline beforeConnectionAction: (() -> Unit)? =
        ConnectionConfig.beforeConnectionAction,
    noinline onSuccessfulConnectionAction: ((response: String) -> Unit)? =
        ConnectionConfig.onSuccessfulConnectionAction,
    noinline onFailureConnectionAction: ((errorMessage: String, errorResponse: String?) -> Unit)? =
        ConnectionConfig.onFailureConnectionAction
): Result<T> {

    return if (isNetworkConnected()) {
        if (predicate == null || predicate()) {

            beforeConnectionAction?.invoke()
            connectionProvider(baseUrl + endPoint).let { connection ->
                try {
                    connection.configure(this)

                    connection.writeStream(this)

                    if (connection.responseCode == HTTP_OK) {

                        connection.readStream(charset).let {
                            onSuccessfulConnectionAction?.invoke(it)
                            Success(connection.responseCode, parser(it))
                        }
                    } else {
                        connection.readErrorStream(charset).let {
                            onFailureConnectionAction?.invoke(connection.responseMessage, it)
                            Error(connection.responseCode, connection.responseMessage, parser(it))
                        }
                    }
                } catch (e: IOException) {
                    connection.readErrorStream(charset).let {
                        onFailureConnectionAction?.invoke(connection.responseMessage, it)
                        Error(connection.responseCode, connection.responseMessage, null)
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } else {
            Error(
                responseCode = ErrorCode.NO_INTERNET.code,
                errorMessage = ErrorCode.NO_INTERNET.message,
                null
            )
        }
    } else {
        Error(
            responseCode = ErrorCode.NO_INTERNET.code,
            errorMessage = ErrorCode.NO_INTERNET.message,
            null
        )
    }
}

private fun isSecure(url: String) = url.contains("https")

private fun doOutput(request: Request) =
    request.postRequestBody != null || request.urlQueryParameters != null

private fun HttpURLConnection.addHeaders(request: Request) {
    ConnectionConfig.defaultHeaders?.forEach { setRequestProperty(it.key, it.value) }
    request.additionalHeaders?.forEach { setRequestProperty(it.key, it.value) }
}

private fun HttpURLConnection.encodeUrlParameters(request: Request) {
    request.urlQueryParameters?.takeUnless { it.isEmpty() }?.let { params ->
        val uriBuilder = Uri.Builder()
        params.forEach { uriBuilder.appendQueryParameter(it.key, it.value) }
        val uriString = uriBuilder.build().toString()
        DataOutputStream(outputStream).use { it.write(uriString.toByteArray(request.charset)) }
    }
}

@PublishedApi
internal fun getConnection(url: String) = if (isSecure(url)) {
    URL(url).openConnection() as HttpsURLConnection
} else {
    URL(url).openConnection() as HttpURLConnection
}

@PublishedApi
internal fun HttpURLConnection.configure(request: Request) {
    readTimeout = ConnectionConfig.readTimeOut
    connectTimeout = ConnectionConfig.connectTimeOut
    requestMethod = request.type.value
    doOutput = doOutput(request)
    addHeaders(request)
    encodeUrlParameters(request)
}

@PublishedApi
internal fun HttpURLConnection.readStream(charset: Charset): String {
    return inputStream.bufferedReader(charset).use(BufferedReader::readText)
}

@PublishedApi
internal fun HttpURLConnection.writeStream(request: Request) {
    request.postRequestBody?.let { data ->
        outputStream.bufferedWriter(request.charset).use {
            it.write(data.toString())
            it.flush()
        }
    }
}

@PublishedApi
internal fun HttpURLConnection.readErrorStream(charset: Charset): String {
    return errorStream?.bufferedReader(charset)?.use(BufferedReader::readText)
        ?: responseMessage.orEmpty()
}