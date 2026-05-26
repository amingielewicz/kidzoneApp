package com.playword.utils

/**
 * Prosty wrapper wyniku operacji asynchronicznej.
 *
 * Używany przez warstwę domain, by repozytoria nie rzucały wyjątków bezpośrednio.
 */
sealed interface OpResult<out T> {
    data class Success<T>(val data: T) : OpResult<T>
    data class Failure(val error: Throwable) : OpResult<Nothing>

    companion object {
        fun <T> success(data: T): OpResult<T> = Success(data)
        fun failure(error: Throwable): OpResult<Nothing> = Failure(error)
    }
}

inline fun <T, R> OpResult<T>.map(transform: (T) -> R): OpResult<R> = when (this) {
    is OpResult.Success -> OpResult.Success(transform(data))
    is OpResult.Failure -> this
}
