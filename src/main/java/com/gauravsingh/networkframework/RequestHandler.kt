package com.gauravsingh.networkframework

import android.net.Uri
import com.gauravsingh.networkframework.ConnectionConfig.isNetworkConnected
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection


fun <T> Request.execute(
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
            connectionProvider(makeUrl(this)).let { connection ->
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

private fun makeUrl(request: Request): String {
    return request.baseUrl + request.endPoint + encodeUrlQueryParams(request)
}

private fun encodeUrlQueryParams(request: Request): String {
    var encodedUrlParams = ""
    request.urlQueryParameters?.entries?.forEachIndexed { index, entry ->
        val key = if (index == 0) "?${entry.key}" else "&${entry.key}"
        encodedUrlParams =
            encodedUrlParams + key + URLEncoder.encode(entry.value, request.charset.name())
    }
    return encodedUrlParams
}

private fun doOutput(request: Request) =
    request.postRequestBody != null || request.urlQueryParameters != null

private fun HttpURLConnection.addHeaders(request: Request) {
    ConnectionConfig.defaultHeaders?.forEach { setRequestProperty(it.key, it.value) }
    request.additionalHeaders?.forEach { setRequestProperty(it.key, it.value) }
}

private fun getConnection(url: String) = URL(url).openConnection()

private fun HttpURLConnection.configure(request: Request) {
    readTimeout = ConnectionConfig.readTimeOut
    connectTimeout = ConnectionConfig.connectTimeOut
    requestMethod = request.type.value
    doOutput = doOutput(request)
    addHeaders(request)
}

private fun HttpURLConnection.readStream(charset: Charset): String {
    return inputStream.bufferedReader(charset).use(BufferedReader::readText)
}

private fun HttpURLConnection.writeStream(request: Request) {
    request.postRequestBody?.let { data ->
        outputStream.bufferedWriter(request.charset).use {
            it.write(data.toString())
            it.flush()
        }
    }
}

private fun HttpURLConnection.readErrorStream(charset: Charset): String {
    return errorStream?.bufferedReader(charset)?.use(BufferedReader::readText)
        ?: responseMessage.orEmpty()
}