package com.ensody.reactivestate

/** Atomically increment this [MutableValueFlow] by [amount]. */
public fun MutableValueFlow<Int>.increment(amount: Int = 1): Int =
    // TODO: We might want to introduce an optimized MutableIntValueFlow backed by an atomic int
    replaceLocked { this + amount }

/** Atomically decrement this [MutableValueFlow] by [amount]. */
public fun MutableValueFlow<Int>.decrement(amount: Int = 1): Int =
    replaceLocked { this - amount }
