package com.haroldadmin.cnradapter

import okhttp3.Headers
import java.io.IOException

sealed interface NetworkResponse<out T : Any, out U : Any> {
    /**
     * A request that resulted in a response with a 2xx status code that has a body.
     */
    data class Success<T : Any>(
        val body: T,
        val headers: Headers? = null,
        val code: Int
    ) : NetworkResponse<T, Nothing>

    /**
     * Describe an error without a specific type.
     * Makes it easier to deal with the case where you just want to know that an error occurred,
     * without knowing the type
     *
     * @example
     * val response = someNetworkAction()
     *
     * when (response) {
     *    is NetworkResponse.Success -> // Action Succeeded do something with body
     *
     *    is NetworkResponse.Error -> // Action failed do something with error
     * }
     */
    sealed interface Error<out U : Any> : NetworkResponse<Nothing, U> {
        val error: Throwable
    }

    /**
     * A request that resulted in a response with a non-2xx status code.
     */
    sealed interface ServerError<out U : Any> : Error<U> {
        val code: Int
        val body: U?
        val headers: Headers?
        override val error: Throwable
            get() = IOException("Network server error: $code \n$body")

        // 4xx Errors
        class BadRequest<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 400
        }
        class NotAuthorized<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 401
        }
        class Forbidden<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 403
        }
        class NotFound<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 404
        }
        class Conflict<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 409
        }

        // 5xx Errors
        class InternalServerError<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 500
        }
        class NotImplemented<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 501
        }
        class BadGateway<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 502
        }
        class ServiceUnavailable<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 503
        }
        class GatewayTimeout<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 504
        }
        class VersionNotSupported<U : Any>(
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U> {
            override val code: Int = 505
        }

        // Unrecognized error code
        class UnrecognizedHttpError<U : Any>(
            override val code: Int,
            override val body: U?,
            override val headers: Headers? = null
        ) : ServerError<U>

        companion object {
            fun <U : Any> fromResult(code: Int, body: U?, headers: Headers?): ServerError<U> = when(code) {
                // 4XX error
                400 -> BadRequest(body, headers)
                401 -> NotAuthorized(body, headers)
                403 -> Forbidden(body, headers)
                404 -> NotFound(body, headers)
                409 -> Conflict(body, headers)

                // 5XX error
                500 -> InternalServerError(body, headers)
                501 -> NotImplemented(body, headers)
                502 -> BadGateway(body, headers)
                503 -> ServiceUnavailable(body, headers)
                504 -> GatewayTimeout(body, headers)
                505 -> VersionNotSupported(body, headers)

                else -> UnrecognizedHttpError(code, body, headers)
            }
        }
    }

    /**
     * A request that didn't result in a response.
     */
    data class NetworkError(override val error: IOException) : Error<Nothing>

    /**
     * A request that resulted in an error different from an IO or Server error.
     *
     * An example of such an error is JSON parsing exception thrown by a serialization library.
     */
    data class UnknownError(
        override val error: Throwable,
        val code: Int? = null,
        val headers: Headers? = null,
    ) : Error<Nothing>
}
