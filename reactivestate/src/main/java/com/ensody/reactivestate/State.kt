package com.ensody.reactivestate

import androidx.lifecycle.SavedStateHandle

// TODO: Add SavedStateHandler-like object for View

/**
 * Base interface for a temporary key-value store.
 *
 * The primary use-case is to abstract away `SavedStateHandle`, so you can write unit tests
 * instead of instrumentation tests and without mocking the Android framework or using Robolectric.
 */
interface LiveDataStore {
    fun contains(key: String): Boolean

    fun <T> getLiveData(key: String, default: T): MutableLiveDataNonNullProxy<T>
}

/** Converts a `SavedStateHandle` to a [LiveDataStore]. */
fun SavedStateHandle.toStore() = SavedStateHandleStore(this)

/** A [LiveDataStore] that wraps a `SavedStateHandle`. */
class SavedStateHandleStore(private val savedStateHandle: SavedStateHandle) : LiveDataStore {
    override fun contains(key: String): Boolean =
        savedStateHandle.contains(key)

    override fun <T> getLiveData(key: String, default: T): MutableLiveDataNonNullProxy<T> =
        savedStateHandle.getLiveDataNonNull(key, default)
}

/** A [LiveDataStore] that can be used e.g. for unit tests. */
class InMemoryStore : LiveDataStore {
    private val store = mutableMapOf<String, MutableLiveDataNonNullProxy<*>>()

    override fun contains(key: String): Boolean = key in store

    @Suppress("UNCHECKED_CAST")
    override fun <T> getLiveData(key: String, default: T): MutableLiveDataNonNullProxy<T> {
        val result = store[key]
        if (result == null) {
            val data = MutableLiveDataNonNull(default).fixValueType()
            store[key] = data
            return data
        }
        return result as MutableLiveDataNonNullProxy<T>
    }
}
