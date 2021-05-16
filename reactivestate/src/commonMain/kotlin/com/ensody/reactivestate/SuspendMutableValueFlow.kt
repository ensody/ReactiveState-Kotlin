package com.ensody.reactivestate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A [StateFlow] that can be mutated only via suspend functions - in contrast to assigning the [value].
 *
 * This is useful e.g. for values backed by some storage/backend system.
 *
 * @see BaseSuspendMutableValueFlow which can be a useful base class for your custom implementation.
 */
@ExperimentalReactiveStateApi
public interface SuspendMutableValueFlow<T> : ValueFlow<T> {
    /**
     * Assigns a new [value].
     *
     * @param value The new value to be assigned.
     * @param force Whether to assign even if value is unchanged. Defaults to `false`, behaving like [MutableStateFlow].
     */
    public suspend fun set(value: T, force: Boolean = false)

    /** Replaces the [value] with [block]'s return value. */
    public suspend fun replace(block: T.() -> T)

    /** Mutates [value] in-place and notifies listeners. The current value is passed as an arg. */
    public suspend fun update(block: (value: T) -> Unit)

    /** Mutates [value] in-place and notifies listeners. The current value is passed via this. */
    public suspend fun updateThis(block: T.() -> Unit)
}

@ExperimentalReactiveStateApi
public fun <T> SuspendMutableValueFlow(value: T, setter: suspend (value: T) -> Unit): SuspendMutableValueFlow<T> =
    SuspendMutableValueFlowImpl(value, setter)

@ExperimentalReactiveStateApi
private class SuspendMutableValueFlowImpl<T>(
    value: T,
    private val setter: suspend (value: T) -> Unit,
) : BaseSuspendMutableValueFlow<T>(value) {

    override suspend fun mutate(value: T) {
        setter(value)
    }
}

/**
 * Base class for [StateFlow]s that can be mutated only via suspend functions - in contrast to assigning the [value].
 *
 * This is useful e.g. for values backed by some storage/backend system.
 *
 * @see SuspendMutableValueFlow for a simple lambda-based implementation.
 */
@ExperimentalReactiveStateApi
public abstract class BaseSuspendMutableValueFlow<T> private constructor(
    private val flow: MutableValueFlow<T>,
) : ValueFlow<T> by flow, SuspendMutableValueFlow<T> {

    public constructor(value: T) : this(MutableValueFlow(value))

    protected abstract suspend fun mutate(value: T)

    public override suspend fun set(value: T, force: Boolean) {
        if (!force && this.value == value) {
            return
        }
        mutate(value)
        flow.emit(value)
    }

    public override suspend fun replace(block: T.() -> T) {
        set(value.block())
    }

    public override suspend fun update(block: (value: T) -> Unit) {
        block(value)
        set(value, force = true)
    }

    public override suspend fun updateThis(block: T.() -> Unit) {
        update(block)
    }
}
