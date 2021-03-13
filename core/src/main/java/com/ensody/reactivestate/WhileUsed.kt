package com.ensody.reactivestate

import kotlinx.coroutines.*

/**
 * A reference-counted value that is created on-demand and freed once nobody uses it.
 *
 * In order to request the value with [invoke] you need a [CoroutineScope].
 * This is used to track the requester's lifetime and in turn the reference count.
 * As an alternative when you don't have a [CoroutineScope] you can also use [disposableValue], but this is more
 * error-prone because it's easier to forget.
 *
 * Typically you'd place such values in your DI system and have one or more ViewModels or UI screens or widgets
 * requesting a value. Once these screens/widgets/ViewModels are destroyed (e.g. because the user pressed on the back
 * button) the value is freed again.
 *
 * This is useful for e.g. caches or other memory-consuming values that shouldn't live forever unnecessarily.
 * Sometimes this can also be helpful for improving security.
 *
 * @param retentionMillis Defines a retention period in milliseconds in which to still keep the value in RAM
 *                        although the reference count returned to 0. If the value is requested again within
 *                        this retention period, the old value is reused. Defaults to 0 (immediately frees the value).
 * @param builder Should create and return the value.
 */
public class WhileUsed<T>(
    private val retentionMillis: Long = 0,
    private val builder: () -> T,
) {
    private var value: Wrapped<T>? = null
    private var references = 0
    private var cleaner: Job? = null

    /**
     * Creates or returns the existing value while incrementing the reference count.
     *
     * When the given [userScope] is canceled the reference count is decremented.
     * Once the count is 0 the value is freed.
     */
    public operator fun invoke(userScope: CoroutineScope): T =
        disposableValue().apply {
            disposeOnCompletionOf(userScope)
        }.value

    /**
     * Creates or returns the existing value while incrementing the reference count. You really want [invoke] instead.
     *
     * IMPORTANT: You have to call `dispose()` on the returned value once you stop using it.
     */
    public fun disposableValue(): DisposableValue<T> {
        synchronized(this) {
            val result = value ?: Wrapped(builder())
            cleaner?.cancel()
            cleaner = null
            value = result
            references++
            return DisposableValue(result.value, ::release)
        }
    }

    private fun release() {
        synchronized(this) {
            references--
            if (references > 0) {
                return
            }
            if (retentionMillis <= 0) {
                value = null
                return
            }
            // While usually it's not ok to use the GlobalScope we really want to make this transparent.
            cleaner = GlobalScope.launch {
                delay(retentionMillis)
                synchronized(this) {
                    if (references == 0) {
                        value = null
                    }
                }
            }
        }
    }
}

private class Wrapped<T>(val value: T)
