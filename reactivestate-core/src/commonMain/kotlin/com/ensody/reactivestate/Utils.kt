package com.ensody.reactivestate

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** A [lazyProperty] that only receives the [KProperty.name] as its argument. */
public inline fun <T> propertyName(crossinline block: (name: String) -> T): ReadOnlyProperty<Any?, T> =
    lazyProperty { block(it.name) }

/** A helper for creating a lazily computed [ReadOnlyProperty] based on a [KProperty]. */
public fun <T> lazyProperty(block: (property: KProperty<*>) -> T): ReadOnlyProperty<Any?, T> =
    LazyProperty(block)

private class LazyProperty<T>(block: (property: KProperty<*>) -> T) : ReadOnlyProperty<Any?, T> {
    private lateinit var property: KProperty<*>
    private val result by lazy { block(property) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        this.property = property
        return result
    }
}

/** Wraps a value. Together with nullability can model an `Option`/`Maybe`. */
public class Wrapped<T>(public val value: T) : Serializable, ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}
