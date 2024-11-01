package com.ensody.reactivestate.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.ensody.reactivestate.afterUpdate
import com.ensody.reactivestate.beforeUpdate
import com.ensody.reactivestate.withSetter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Converts this [MutableStateFlow] to a [MutableState].
 */
@Composable
public fun <T> MutableStateFlow<T>.collectAsMutableState(
    context: CoroutineContext = EmptyCoroutineContext,
): MutableState<T> =
    collectAsState(context).toMutable { this@collectAsMutableState.value = it }

/**
 * Returns a new [MutableState] that calls [setter] before doing the actual value update.
 *
 * The value is set automatically for you after [setter] has been called. For more control use [withSetter].
 * This can be used to wrap a [State]/[MutableState] with extra update logic.
 */
@Composable
public fun <T> MutableState<T>.beforeUpdate(setter: MutableState<T>.(T) -> Unit): MutableState<T> =
    withSetter {
        setter(it)
        value = it
    }

/**
 * Returns a new [MutableState] that calls [setter] after doing the actual value update.
 *
 * The value is set automatically for you before [setter] has been called. For more control use [withSetter].
 * This can be used to wrap a [State]/[MutableState] with extra update logic.
 */
@Composable
public fun <T> MutableState<T>.afterUpdate(setter: MutableState<T>.(T) -> Unit): MutableState<T> =
    withSetter {
        value = it
        setter(it)
    }

/**
 * Returns a new [MutableState] that calls [setter] instead of doing the actual value update.
 *
 * IMPORTANT: You must manually set `value =` on the underlying [MutableState].
 * The [setter] gets the underlying [MutableState] via `this`.
 *
 * This can be used to wrap a [MutableState] with extra update logic.
 * For simpler use cases you might prefer [beforeUpdate]/[afterUpdate] instead.
 */
@Composable
public fun <T> MutableState<T>.withSetter(setter: MutableState<T>.(T) -> Unit): MutableState<T> =
    MutableStateInterceptor(state = this, setter = setter)

/**
 * Converts this [State] to a [MutableState] that calls [setter] for doing the actual value update.
 */
@Composable
public fun <T> State<T>.toMutable(setter: State<T>.(T) -> Unit): MutableState<T> =
    StateInterceptor(state = this, setter = setter)

private class MutableStateInterceptor<T>(
    private val state: MutableState<T>,
    private val setter: MutableState<T>.(T) -> Unit,
) : MutableState<T> {

    override var value: T
        get() = state.value
        set(value) {
            state.setter(value)
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = ::value::set
}

private class StateInterceptor<T>(
    private val state: State<T>,
    private val setter: State<T>.(T) -> Unit,
) : MutableState<T> {

    override var value: T
        get() = state.value
        set(value) {
            state.setter(value)
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = ::value::set
}
