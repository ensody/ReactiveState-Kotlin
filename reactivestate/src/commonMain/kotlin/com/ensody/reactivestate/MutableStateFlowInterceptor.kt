package com.ensody.reactivestate

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Returns a new [MutableStateFlow] that calls [setter] before doing the actual value update.
 *
 * The value is set automatically for you after [setter] has been called. For more control use [withSetter].
 * This can be used to wrap a [MutableStateFlow] with extra update logic.
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
 * This can be used to wrap a [MutableStateFlow] with extra update logic.
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
 * For simpler use cases you might prefer [withSetter] instead.
 */
public fun <T> MutableStateFlow<T>.withSetter(
    setter: MutableStateFlow<T>.(T) -> Unit,
): MutableStateFlow<T> =
    MutableStateFlowInterceptor(this, setter)

private class MutableStateFlowInterceptor<T>(
    private val delegate: MutableStateFlow<T>,
    private val setter: MutableStateFlow<T>.(T) -> Unit,
) : MutableStateFlow<T> by delegate {
    override var value: T
        get() = delegate.value
        set(value) { delegate.setter(value) }
}
