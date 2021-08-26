package com.payco.paycopaymentdemo.utils

import java.util.regex.Pattern

enum class ClientType {
    WEB,
    JAVASCRIPT,
    BLANK,
    APP
    ;

    companion object {
        fun getClientType(url: String): ClientType {
            val httpMatcher = Pattern.compile(
                "^(https?):\\/\\/([^:\\/\\s]+)(/?)"
            ).matcher(url)

            return when {
                httpMatcher.find() -> WEB
                url.startsWith("javascript:") -> JAVASCRIPT
                url.startsWith("about:blank") -> BLANK

                else -> APP
            }
        }
    }
}
