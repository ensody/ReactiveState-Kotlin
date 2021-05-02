package com.ensody.reactivestate

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Base interface for a temporary observable key-value store.
 *
 * This is useful for multi-platform projects and in general for abstracting away `SavedStateHandle`,
 * so you can write tests without Robolectric.
 */
public interface StateFlowStore {
    public operator fun contains(key: String): Boolean

    public fun <T> getData(key: String, default: T): MutableValueFlow<T>
}

/** For use with `by` delegation. Returns the [StateFlowStore] entry for the key that equals the property name. */
public fun <T> StateFlowStore.getData(default: T): ReadOnlyProperty<Any?, MutableValueFlow<T>> =
    StateFlowStoreProperty(lazy { this }, default)

public class StateFlowStoreProperty<T>(store: Lazy<StateFlowStore>, default: T) :
    ReadOnlyProperty<Any?, MutableValueFlow<T>> {

    private lateinit var key: String
    private val data by lazy { store.value.getData(key, default) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): MutableValueFlow<T> {
        key = property.name
        return data
    }
}

/** A [StateFlowStore] that can be used for unit tests or non-Android parts of multi-platform projects. */
public class InMemoryStateFlowStore : StateFlowStore {
    private val store = mutableMapOf<String, MutableValueFlow<*>>()

    override fun contains(key: String): Boolean = key in store

    override fun <T> getData(key: String, default: T): MutableValueFlow<T> =
        getData(key, default, null)

    public fun <T> getData(key: String, default: T, setter: ((value: T) -> Unit)?): MutableValueFlow<T> =
        store.getOrPut(key) {
            val data = MutableValueFlow(default, setter)
            store[key] = data
            data
        } as MutableValueFlow<T>
}

/**
 * A wrapper [StateFlowStore] that prefixes every key with a namespace.
 *
 * This is useful for preventing name clashes when passing [StateFlowStore]s to sub-components.
 */
public class NamespacedStateFlowStore(
    private val store: StateFlowStore,
    private val namespace: String,
) : StateFlowStore {
    override fun contains(key: String): Boolean = encodeKey(key) in store

    override fun <T> getData(key: String, default: T): MutableValueFlow<T> =
        store.getData(encodeKey(key), default)

    private fun encodeKey(key: String): String =
        "$namespace<<$key"
}
