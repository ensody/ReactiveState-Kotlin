package com.ensody.reactivestate

import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmName

/** Atomically increment this [MutableValueFlow] by [amount]. */
public fun MutableValueFlow<Int>.increment(amount: Int = 1): Int =
    // TODO: We might want to introduce an optimized MutableIntValueFlow backed by an atomic int
    replaceLocked { this + amount }

/** Atomically decrement this [MutableValueFlow] by [amount]. */
public fun MutableValueFlow<Int>.decrement(amount: Int = 1): Int =
    increment(-amount)

/**
 * Keeps this [MutableValueFlow] [incremented][increment] by the latest value in the given [flow].
 *
 * For example, if this [MutableValueFlow] is initially set to 1 and [flow] is set to 1, 2, 0 then the value
 * of this [MutableValueFlow] will be set to 2, then 3, then 1.
 */
public suspend fun MutableValueFlow<Int>.incrementFrom(flow: StateFlow<Int>) {
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
 * Keeps this [MutableValueFlow] [incremented][increment] by the latest value in the given [flow].
 *
 * For example, if this [MutableValueFlow] is initially set to 1 and [flow] is set to true, false, true then the value
 * of this [MutableValueFlow] will be set to 2, then 1, then 2.
 */
@JvmName("incrementFromBoolean")
public suspend fun MutableValueFlow<Int>.incrementFrom(flow: StateFlow<Boolean>) {
    incrementFrom(derived { if (get(flow)) 1 else 0 })
}
