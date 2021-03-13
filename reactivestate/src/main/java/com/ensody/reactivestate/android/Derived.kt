package com.ensody.reactivestate.android

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.ensody.reactivestate.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 */
public fun <T> ViewModel.derived(
    started: SharingStarted,
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(viewModelScope),
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(started = started, observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * @param initial The initial value (until the first computation finishes).
 * @param launcher The [CoroutineLauncher] to use.
 * @param flowTransformer How changes should be executed/collected. Defaults to `{ conflatedWorker() }`.
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Whether loading state may be tracked for the (re-)computation. Defaults to `true`.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> ViewModel.derived(
    initial: T,
    started: SharingStarted,
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(viewModelScope),
    flowTransformer: AutoRunFlowTransformer = defaultAutoRunFlowTransformer,
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: Boolean = true,
    observer: CoAutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(
        initial = initial,
        started = started,
        flowTransformer = flowTransformer,
        dispatcher = dispatcher,
        withLoading = withLoading,
        observer = observer,
    )

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 */
public fun <T> LifecycleOwner.derived(
    started: SharingStarted,
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(lifecycleScope),
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(started = started, observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * @param initial The initial value (until the first computation finishes).
 * @param launcher The [CoroutineLauncher] to use.
 * @param flowTransformer How changes should be executed/collected. Defaults to `{ conflatedWorker() }`.
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Whether loading state may be tracked for the (re-)computation. Defaults to `true`.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> LifecycleOwner.derived(
    initial: T,
    started: SharingStarted,
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(lifecycleScope),
    flowTransformer: AutoRunFlowTransformer = defaultAutoRunFlowTransformer,
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: Boolean = true,
    observer: CoAutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(
        initial = initial,
        started = started,
        flowTransformer = flowTransformer,
        dispatcher = dispatcher,
        withLoading = withLoading,
        observer = observer,
    )
