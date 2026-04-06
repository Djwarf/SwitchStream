package com.switchsides.switchstream.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun isNetworkError(e: Throwable): Boolean {
    return e is IOException || e is ConnectException ||
        e is UnknownHostException || e is SocketTimeoutException ||
        e.cause?.let { isNetworkError(it) } == true
}

fun offlineErrorMessage(fallback: String = ""): String {
    return "You're offline"
}
