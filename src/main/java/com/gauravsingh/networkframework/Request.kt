package com.gauravsingh.networkframework

import org.json.JSONObject
import java.nio.charset.Charset

data class Request(
    val endPoint: String,
    val type: RequestType,
    val additionalHeaders: Headers? = null,
    val urlQueryParameters: UrlQueryParameters? = null,
    val postRequestBody: JSONObject? = null,
    val baseUrl: String = ConnectionConfig.baseUrl,
    val charset: Charset = Charsets.UTF_8
)

typealias UrlQueryParameters = HashMap<String, String>