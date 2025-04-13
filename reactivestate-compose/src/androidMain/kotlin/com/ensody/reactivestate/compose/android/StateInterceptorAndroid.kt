package com.ensody.reactivestate.compose.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ensody.reactivestate.compose.toMutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Converts this [MutableStateFlow] to a [MutableState].
 */
@Composable
public fun <T> MutableStateFlow<T>.collectAsMutableStateWithLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
): MutableState<T> =
    collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner,
        minActiveState = minActiveState,
        context = context,
    ).toMutable { this@collectAsMutableStateWithLifecycle.value = it }

/**
 * Converts this [MutableStateFlow] to a [MutableState].
 */
@Composable
public fun <T> MutableStateFlow<T>.collectAsMutableStateWithLifecycle(
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
): MutableState<T> =
    collectAsStateWithLifecycle(
        lifecycle = lifecycle,
        minActiveState = minActiveState,
        context = context,
    ).toMutable { this@collectAsMutableStateWithLifecycle.value = it }

/**
 * Converts this [MutableStateFlow] to a [MutableState].
 */
@Composable
public fun <T> MutableStateFlow<T>.collectAsMutableStateWithLifecycle(
    initialValue: T,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
): MutableState<T> =
    collectAsStateWithLifecycle(
        initialValue = initialValue,
        lifecycleOwner = lifecycleOwner,
        minActiveState = minActiveState,
        context = context,
    ).toMutable { this@collectAsMutableStateWithLifecycle.value = it }

/**
 * Converts this [MutableStateFlow] to a [MutableState].
 */
@Composable
public fun <T> MutableStateFlow<T>.collectAsMutableStateWithLifecycle(
    initialValue: T,
    lifecycle: Lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
): MutableState<T> =
    collectAsStateWithLifecycle(
        initialValue = initialValue,
        lifecycle = lifecycle,
        minActiveState = minActiveState,
        context = context,
    ).toMutable { this@collectAsMutableStateWithLifecycle.value = it }
