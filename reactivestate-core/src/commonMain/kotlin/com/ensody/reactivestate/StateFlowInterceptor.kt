package com.ensody.reactivestate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Returns a new [MutableStateFlow] that calls [setter] before doing the actual value update.
 *
 * The value is set automatically for you after [setter] has been called. For more control use [withSetter].
 * This can be used to wrap a [StateFlow]/[MutableStateFlow] with extra update logic.
 */
public fun <T> MutableStateFlow<T>.beforeUpdate(setter: (T) -> Unit): MutableStateFlow<T> =
    withSetter {
        setter(it)
        value = it
    }

/**
 * Returns a new [MutableStateFlow] that calls [setter] after doing the actual value update.
 *
 * The value is set automatically for you before [setter] has been called. For more control use [withSetter].
 * This can be used to wrap a [StateFlow]/[MutableStateFlow] with extra update logic.
 */
public fun <T> MutableStateFlow<T>.afterUpdate(setter: (T) -> Unit): MutableStateFlow<T> =
    withSetter {
        value = it
        setter(it)
    }

/**
 * Returns a new [MutableStateFlow] that calls [setter] instead of doing the actual value update.
 *
 * IMPORTANT: You must manually set `value =` on the underlying [MutableStateFlow].
 * The [setter] gets the underlying [MutableStateFlow] via `this`.
 *
 * This can be used to wrap a [MutableStateFlow] with extra update logic.
 * For simpler use cases you might prefer [beforeUpdate]/[afterUpdate] instead.
 */
public fun <T> MutableStateFlow<T>.withSetter(
    setter: MutableStateFlow<T>.(T) -> Unit,
): MutableStateFlow<T> =
    MutableStateFlowInterceptor(this, setter)

/**
 * Converts this [StateFlow] to a [MutableStateFlow] that calls [setter] for doing the actual value update.
 */
public fun <T> StateFlow<T>.toMutable(
    setter: StateFlow<T>.(T) -> Unit,
): MutableStateFlow<T> =
    StateFlowInterceptor(this, setter)

private class MutableStateFlowInterceptor<T>(
    private val delegate: MutableStateFlow<T>,
    private val setter: MutableStateFlow<T>.(T) -> Unit,
) : MutableStateFlow<T> by delegate {
    override var value: T
        get() = delegate.value
        set(value) { delegate.setter(value) }
}

private class StateFlowInterceptor<T>(
    private val delegate: StateFlow<T>,
    private val setter: StateFlow<T>.(T) -> Unit,
) : MutableStateFlow<T> {

    private val mutex = Mutex()

    override val replayCache: List<T> get() = delegate.replayCache

    override val subscriptionCount = MutableStateFlow(0)

    override var value: T
        get() = delegate.value
        set(value) { delegate.setter(value) }

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        subscriptionCount.increment()
        try {
            delegate.collect(collector)
        } finally {
            subscriptionCount.decrement()
        }
    }

    override fun compareAndSet(expect: T, update: T): Boolean =
        mutex.withSpinLock {
            if (value == expect) {
                value = update
                true
            } else {
                false
            }
        }

    @ExperimentalCoroutinesApi
    override fun resetReplayCache() {
        throw UnsupportedOperationException("MutableStateFlow doesn't support resetReplayCache")
    }

    override fun tryEmit(value: T): Boolean {
        this.value = value
        return true
    }

    override suspend fun emit(value: T) {
        this.value = value
    }
}
