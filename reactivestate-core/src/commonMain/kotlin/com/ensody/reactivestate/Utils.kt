package com.ensody.reactivestate

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/** A [lazyProperty] that only receives the [KProperty.name] as its argument. */
public inline fun <T> propertyName(crossinline block: (name: String) -> T): ReadOnlyProperty<Any?, T> =
    lazyProperty { block(it.name) }

/** A helper for creating a lazily computed [ReadOnlyProperty] based on a [KProperty]. */
public fun <T> lazyProperty(block: (property: KProperty<*>) -> T): LazyProperty<T> =
    LazyPropertyImpl(block)

public interface LazyProperty<T> : ReadOnlyProperty<Any?, T>

private class LazyPropertyImpl<T>(block: (property: KProperty<*>) -> T) : LazyProperty<T> {
    private lateinit var property: KProperty<*>
    private val result by lazy { block(property) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        this.property = property
        return result
    }
}

/** Wraps a value. Together with nullability can model an `Option`/`Maybe`. */
public data class Wrapped<T>(public val value: T) : JvmSerializable, ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun toString(): String =
        "Wrapped($value)"
}

/** Returns the result of [block] if [value] is true, else null. Similar to [takeIf], but flipped arguments. */
public inline fun <T> ifTake(value: Boolean, block: () -> T): T? =
    if (value) block() else null

/** Returns the result of [block] if [value] is false, else null. Similar to [takeUnless], but flipped arguments. */
public inline fun <T> unlessTake(value: Boolean, block: () -> T): T? =
    if (!value) block() else null

/** Returns the result of [block] if [value] is true, else null. Similar to [run], but executes conditionally. */
public inline fun <T, R> T.runIf(value: Boolean, block: T.() -> R): R? =
    if (value) block() else null

/** Executes [block] if [value] is true, else just returns `this`. Similar to [apply], but executes conditionally. */
public inline fun <T> T.applyIf(value: Boolean, block: T.() -> Unit): T =
    if (value) apply(block) else this

public expect val KClass<*>.qualifiedNameOrSimpleName: String?
