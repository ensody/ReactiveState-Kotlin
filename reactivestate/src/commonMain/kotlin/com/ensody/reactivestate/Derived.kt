package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 *
 * This behaves like [SharingStarted.Eagerly] and computes the initial value by executing the [observer] function
 * immediately.
 */
public fun <T> CoroutineLauncher.derived(observer: AutoRunCallback<T>): StateFlow<T> {
    val onChangeFlow = MutableFlow<Unit>(Channel.CONFLATED)
    val autoRunner = AutoRunner(launcher = this, onChange = { onChangeFlow.tryEmit(Unit) }, observer = observer)
    val flow = onChangeFlow.onCompletion { autoRunner.dispose() }.map { autoRunner.run() }
    return flow.stateIn(scope = launcherScope, started = SharingStarted.Eagerly, initialValue = autoRunner.run())
}

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 *
 * This behaves like [SharingStarted.Eagerly] and computes the initial value by executing the [observer] function
 * immediately.
 */
public fun <T> CoroutineScope.derived(
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * You can use this to compute values on-demand only via [SharingStarted.WhileSubscribed].
 *
 * @param initial The initial value (until the first computation finishes).
 * @param started When the value should be updated. Pass [SharingStarted.WhileSubscribed] to compute only on demand.
 *                Defaults to [SharingStarted.Eagerly].
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.loading].
 * @param observer The callback which is used to track the observables.
 */
public fun <T> CoroutineLauncher.derived(
    initial: T,
    started: SharingStarted = SharingStarted.Eagerly,
    flowTransformer: DerivedFlowTransformer<T> = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: MutableValueFlow<Int>? = loading,
    observer: CoAutoRunCallback<T>,
): StateFlow<T> {
    val onChangeFlow = MutableFlow<Unit>(Channel.CONFLATED)
    onChangeFlow.tryEmit(Unit)
    val autoRunner =
        CoAutoRunner(
            launcher = this,
            onChange = { onChangeFlow.tryEmit(Unit) },
            flowTransformer = Flow<Unit>::transform,
            dispatcher = dispatcher,
            withLoading = null,
            observer = observer,
        )
    val flow = onChangeFlow.onCompletion { autoRunner.dispose() }.flowTransformer {
        track(withLoading = withLoading) {
            emit(autoRunner.run())
        }
    }
    return flow.stateIn(scope = launcherScope, started = started, initialValue = initial)
}

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
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to `null`.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> CoroutineScope.derived(
    initial: T,
    started: SharingStarted = SharingStarted.Eagerly,
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    flowTransformer: DerivedFlowTransformer<T> = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: MutableValueFlow<Int>? = null,
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
