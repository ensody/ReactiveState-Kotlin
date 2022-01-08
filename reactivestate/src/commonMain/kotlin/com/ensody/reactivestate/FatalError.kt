package com.ensody.reactivestate

public expect fun Throwable.isFatal(): Boolean

/** Throws this exception if it's fatal. Otherwise returns it. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T: Throwable> T.throwIfFatal(): T =
    if (isFatal()) throw this else this

/** Similar to the stdlib [runCatching], but uses [throwIfFatal] to re-throw fatal exceptions immediately. */
public inline fun <T> runCatchingNonFatal(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e.throwIfFatal())
    }
