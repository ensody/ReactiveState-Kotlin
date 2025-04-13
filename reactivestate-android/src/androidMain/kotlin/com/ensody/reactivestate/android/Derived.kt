package com.ensody.reactivestate.android

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ensody.reactivestate.AutoRunCallback
import com.ensody.reactivestate.AutoRunFlowTransformer
import com.ensody.reactivestate.CoAutoRunCallback
import com.ensody.reactivestate.CoroutineLauncher
import com.ensody.reactivestate.SimpleCoroutineLauncher
import com.ensody.reactivestate.autoRun
import com.ensody.reactivestate.coAutoRun
import com.ensody.reactivestate.conflatedWorker
import com.ensody.reactivestate.derived
import com.ensody.reactivestate.dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 *
 * This behaves like [SharingStarted.Eagerly] and computes the initial value by executing the [observer] function
 * immediately.
 */
public fun <T> ViewModel.derived(
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(viewModelScope),
    synchronous: Boolean = true,
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(synchronous = synchronous, observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * You can use this to compute values on-demand only via [SharingStarted.WhileSubscribed].
 *
 * @param initial The initial value (until the first computation finishes).
 * @param started When the value should be updated. Pass [SharingStarted.WhileSubscribed] to compute only on demand.
 *                Defaults to [SharingStarted.Eagerly].
 * @param launcher The [CoroutineLauncher] to use.
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.main`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.loading] if
 *                    this is a [CoroutineLauncher] or `null` otherwise.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> ViewModel.derived(
    initial: T,
    started: SharingStarted = SharingStarted.Eagerly,
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(viewModelScope),
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.main,
    withLoading: MutableStateFlow<Int>? = if (this is CoroutineLauncher) launcher.loading else null,
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
 *
 * This behaves like [SharingStarted.Eagerly] and computes the initial value by executing the [observer] function
 * immediately.
 */
public fun <T> LifecycleOwner.derived(
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else LifecycleCoroutineLauncher(this),
    synchronous: Boolean = true,
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(synchronous = synchronous, observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * You can use this to compute values on-demand only via [SharingStarted.WhileSubscribed].
 *
 * @param initial The initial value (until the first computation finishes).
 * @param started When the value should be updated. Pass [SharingStarted.WhileSubscribed] to compute only on demand.
 *                Defaults to [SharingStarted.Eagerly].
 * @param launcher The [CoroutineLauncher] to use.
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.main`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.loading] if
 *                    this is a [CoroutineLauncher] or `null` otherwise.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> LifecycleOwner.derived(
    initial: T,
    started: SharingStarted = SharingStarted.Eagerly,
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else LifecycleCoroutineLauncher(this),
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.main,
    withLoading: MutableStateFlow<Int>? = if (this is CoroutineLauncher) launcher.loading else null,
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
