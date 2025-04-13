package com.ensody.reactivestate

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlin.jvm.JvmName

/**
 * A version of [update] where the current value is passed via `this`.
 *
 * This is a simple helper for the common case where you want to `copy()` a data class:
 *
 * ```kotlin
 * data class Foo(val num: Int)
 *
 * val stateFlow = MutableStateFlow(Foo(3))
 * stateFlow.replace { copy(num = 5) }
 * ```
 */
public fun <T> MutableStateFlow<T>.replace(block: T.() -> T) {
    update(block)
}

/**
 * A version of [getAndUpdate] where the current value is passed via `this`.
 *
 * This is a simple helper for the common case where you want to `copy()` a data class:
 *
 * ```kotlin
 * data class Foo(val num: Int)
 *
 * val stateFlow = MutableStateFlow(Foo(3))
 * val oldValue = stateFlow.getAndReplace { copy(num = 5) }
 * ```
 *
 * @return The previous value before replacing.
 */
public fun <T> MutableStateFlow<T>.getAndReplace(block: T.() -> T): T =
    getAndUpdate(block)

/**
 * This is a version of [updateAndGet].
 *
 * This is a simple helper for the common case where you want to `copy()` a data class:
 *
 * ```kotlin
 * data class Foo(val num: Int)
 *
 * val stateFlow = MutableStateFlow(Foo(3))
 * val newValue = stateFlow.replaceAndGet { copy(num = 5) }
 * ```
 *
 * @return The new value after replacing.
 */
public fun <T> MutableStateFlow<T>.replaceAndGet(block: T.() -> T): T =
    updateAndGet(block)

/**
 * Similar to [MutableStateFlow.onSubscription] but returns a [MutableStateFlow].
 */
public fun <T> MutableStateFlow<T>.onStateSubscription(
    block: suspend FlowCollector<T>.() -> Unit,
): MutableStateFlow<T> =
    onSubscription(block).stateOnDemand { value }.toMutable { this@onStateSubscription.value = it }

/**
 * Similar to [StateFlow.onSubscription] but returns a [StateFlow].
 */
public fun <T> StateFlow<T>.onStateSubscription(
    block: suspend FlowCollector<T>.() -> Unit,
): StateFlow<T> =
    onSubscription(block).stateOnDemand { value }

/** Atomically increments this [MutableStateFlow] by [amount] (default 1) and returns previous value. */
public fun MutableStateFlow<Int>.increment(amount: Int = 1): Int =
    getAndUpdate { it + amount }

/** Atomically decrements this [MutableStateFlow] by [amount] (default 1) and returns previous value. */
public fun MutableStateFlow<Int>.decrement(amount: Int = 1): Int =
    increment(-amount)

/**
 * Keeps this [MutableStateFlow] [incremented][increment] by the latest value in the given [flow].
 *
 * For example, if this [MutableStateFlow] is initially set to 1 and [flow] is set to 1, 2, 0 then the value
 * of this [MutableStateFlow] will be set to 2, then 3, then 1.
 */
public suspend fun MutableStateFlow<Int>.incrementFrom(flow: StateFlow<Int>) {
    var previous = 0
    try {
        flow.collect {
            increment(it - previous)
            previous = it
        }
    } finally {
        decrement(previous)
    }
}

/**
 * Keeps this [MutableStateFlow] [incremented][increment] by the latest value in the given [flow].
 *
 * For example, if this [MutableStateFlow] is initially set to 1 and [flow] is set to true, false, true then the value
 * of this [MutableStateFlow] will be set to 2, then 1, then 2.
 */
@JvmName("incrementFromBoolean")
public suspend fun MutableStateFlow<Int>.incrementFrom(flow: StateFlow<Boolean>) {
    incrementFrom(derived { if (get(flow)) 1 else 0 })
}
