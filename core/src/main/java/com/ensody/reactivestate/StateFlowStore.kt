package com.ensody.reactivestate

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Base interface for a temporary observable key-value store.
 *
 * This is useful for multi-platform projects and in general for abstracting away `SavedStateHandle`,
 * so you can write unit tests (instead of instrumentation tests) without using Robolectric.
 */
interface StateFlowStore {
    fun contains(key: String): Boolean

    fun <T> getData(key: String, default: T): MutableStateFlow<T>
}

/** A [StateFlowStore] that can be used for unit tests or non-Android parts of multi-platform projects. */
class InMemoryStateFlowStore : StateFlowStore {
    private val store = mutableMapOf<String, MutableStateFlow<*>>()

    override fun contains(key: String): Boolean = key in store

    @Suppress("UNCHECKED_CAST")
    override fun <T> getData(key: String, default: T): MutableStateFlow<T> =
        store.getOrPut(key) {
            val data = MutableStateFlow(default)
            store[key] = data
            data
        } as MutableStateFlow<T>
}
