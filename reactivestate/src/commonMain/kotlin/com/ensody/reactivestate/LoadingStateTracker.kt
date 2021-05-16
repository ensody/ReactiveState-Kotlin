package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope

/**
 * A [ReducingStateFlow] used for tracking loading states.
 *
 * Use [createLoadingState] to create a new [MutableValueFlow] for loading state tracking.
 * Make sure use use [MutableValueFlow.replaceLocked] to increment/decrement the loading state.
 */
public class LoadingStateTracker(scope: CoroutineScope) :
    ReducingStateFlow<Int, Int>(scope, { it.sum() }) {

    /** Creates and tracks a new loading state. */
    public fun createLoadingState(): MutableValueFlow<Int> =
        MutableValueFlow(0).also { add(it) }
}

/** Atomically increment this [MutableValueFlow]. */
public fun MutableValueFlow<Int>.increment() {
    // TODO: We might want to introduce an optimized MutableIntValueFlow backed by an atomic int
    replaceLocked { this + 1 }
}

/** Atomically decrement this [MutableValueFlow]. */
public fun MutableValueFlow<Int>.decrement() {
    replaceLocked { this - 1 }
}
