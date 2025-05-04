package com.ensody.reactivestate

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A suspend/coroutine version of [Lazy].
 */
public class LazySuspend<T>(private val block: suspend () -> T) {
    private var value: Result<T>? = null
    private val mutex = Mutex()

    public suspend fun get(): T {
        if (value?.isSuccess != true) {
            mutex.withLock {
                // In case after getting the lock we have a successful result
                value?.takeIf { it.isSuccess }?.let {
                    return it.getOrThrow()
                }
                // If there's no value or the last run caused an exception
                value = runCatching { block() }
            }
        }
        @Suppress("UnsafeCallOnNullableType")
        return value!!.getOrThrow()
    }

    public fun reset() {
        value = null
    }
}
