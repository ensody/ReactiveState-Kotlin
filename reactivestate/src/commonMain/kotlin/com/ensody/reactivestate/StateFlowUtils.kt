package com.ensody.reactivestate

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Replaces the [MutableStateFlow.value] with [block]'s return value.
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
    value = value.block()
}
