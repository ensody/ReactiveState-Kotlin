package com.ensody.reactivestate

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

/** A version of [StateFlow] that doesn't compare against previous values. */
public interface ValueFlow<T> : StateFlow<T>

/**
 * A version of [MutableStateFlow] that provides better support for mutable values via the [update] operation.
 * Assigning to `.value` still has `distinctUntilChanged` behavior, but `emit`/`tryEmit` and [update] always trigger
 * a change event.
 *
 * Example of mutating the `value` in-place:
 *
 * ```
 * flow.update {
 *     it.subvalue1.deepsubvalue.somevalue += 3
 *     it.subvalue2.state = SomeState.IN_PROGRESS
 *     it.isLoading = true
 * }
 * ```
 *
 * Why is this needed?
 *
 * In Kotlin, working with nested immutable values (e.g. nested data class with val) is very unwieldy because you have
 * to manually copy each element and its children:
 *
 * ```
 * flow.value = flow.value.let {
 *     it.copy(
 *         subvalue1 = it.subvalue1.copy(
 *             deepsubvalue = it.subvalue1.deepsubvalue.copy(somevalue = it.subvalue1.deepsubvalue.somevalue + 3)
 *          ),
 *         subvalue2 = it.subvalue2.copy(state = SomeState.IN_PROGRESS),
 *         isLoading = true,
 *     )
 * }
 * ```
 *
 * In many cases the UI state is even held in mutable data classes (with var), but doing the following would be unsafe
 * with [MutableStateFlow] because the value is considered unchanged, so this code won't trigger a UI update:
 *
 * ```
 * flow.value = flow.value.also {
 *     it.subvalue1.deepsubvalue.somevalue += 3
 *     it.subvalue2.state = SomeState.IN_PROGRESS
 *     it.isLoading = true
 * }
 * ```
 *
 * Kotlin just isn't a pure functional language with built-in lens support and we have to deal with mutable values,
 * so we should prevent overly complicated code with nested copy() and stupid mistakes like missing UI updates.
 *
 * That's why [MutableValueFlow] tries to make working with mutable values easy and safe.
 */
public interface MutableValueFlow<T> : ValueFlow<T>, MutableStateFlow<T> {
    /** Mutates [value] in-place and notifies listeners. The current value is passed as an arg. */
    public fun update(block: (value: T) -> Unit)

    /** Mutates [value] in-place and notifies listeners. The current value is passed via this. */
    public fun updateThis(block: T.() -> Unit) {
        update(block)
    }

    /**
     * Replaces the [value] with [block]'s return value. This is safe under concurrency.
     *
     * This is a simple helper for the common case where you want to `copy()` a data class:
     *
     * ```
     * data class Foo(val num: Int)
     *
     * val stateFlow = MutableStateFlow(Foo(3))
     * stateFlow.replace { copy(num = 5) }
     * ```
     */
    public fun replaceLocked(block: T.() -> T)
}

/** Instantiates a [MutableValueFlow] with the given initial [value] and optional [setter] to intercept mutations. */
public fun <T> MutableValueFlow(value: T, setter: ((value: T) -> Unit)? = null): MutableValueFlow<T> =
    ValueFlowImpl(initial = value, setter = setter)

// XXX: The MutableValueFlow, MutableSharedFlow and Flow interfaces aren't stable for inheritance, yet.
// We use delegation to ensure that at least up to MutableSharedFlow we implement the whole interface automatically.
// Only changes to the very tiny (Mutable)StateFlow interface can still lead to incompatibility on our side.
private class ValueFlowImpl<T> constructor(
    private val flow: MutableSharedFlow<T>,
    initial: T,
    private val setter: ((value: T) -> Unit)?,
) : MutableValueFlow<T>, MutableSharedFlow<T> by flow {

    constructor(initial: T, setter: ((value: T) -> Unit)?) : this(
        flow = MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST),
        initial = initial,
        setter = setter,
    )

    private val mutex by lazy { Mutex() }

    init {
        tryEmit(initial)
    }

    override fun update(block: (value: T) -> Unit) {
        mutex.withSpinLock {
            block(value)
            tryEmit(value)
        }
    }

    override fun replaceLocked(block: T.() -> T) {
        mutex.withSpinLock {
            val newValue = value.block()
            if (value != newValue) {
                tryEmit(newValue)
            }
        }
    }

    override fun resetReplayCache() {
        throw UnsupportedOperationException()
    }

    override var value: T
        get() = replayCache.first()
        set(newValue) {
            mutex.withSpinLock {
                if (value != newValue) {
                    tryEmit(newValue)
                }
            }
        }

    override fun compareAndSet(expect: T, update: T): Boolean =
        mutex.withSpinLock {
            if (value == expect) {
                tryEmit(update)
                true
            } else {
                false
            }
        }

    override suspend fun emit(value: T) {
        setter?.invoke(value)
        flow.emit(value)
    }

    override fun tryEmit(value: T): Boolean {
        setter?.invoke(value)
        return flow.tryEmit(value)
    }
}
