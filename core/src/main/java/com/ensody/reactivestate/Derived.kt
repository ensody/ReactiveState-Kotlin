package com.ensody.reactivestate

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 */
public fun <T> CoroutineLauncher.derived(started: SharingStarted, observer: AutoRunCallback<T>): StateFlow<T> {
    var onChange: () -> Unit = {}
    val autoRunner = AutoRunner(this, onChange = { onChange() }, observer = observer)
    val initialValue = CompletableDeferred<T>()
    val flow = callbackFlow {
        onChange = {
            sendBlocking(autoRunner.run())
        }
        if (started === SharingStarted.Eagerly) {
            send(initialValue.await())
        } else {
            send(autoRunner.run())
        }
        awaitClose { autoRunner.dispose() }
    }
    val realInitialValue = autoRunner.run(track = started === SharingStarted.Eagerly)
    initialValue.complete(realInitialValue)
    return flow.stateIn(launcherScope, started = started, initialValue = realInitialValue)
}

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 */
public fun <T> CoroutineScope.derived(
    started: SharingStarted,
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(started = started, observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * @param initial The initial value (until the first computation finishes).
 * @param flowTransformer How changes should be collected. Defaults to `{ mapLatest { } }`.
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Whether loading state may be tracked for the (re-)computation. Defaults to `true`.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> CoroutineLauncher.derived(
    initial: T,
    started: SharingStarted,
    flowTransformer: AutoRunFlowTransformer = { mapLatest { } },
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: Boolean = true,
    observer: CoAutoRunCallback<T>,
): StateFlow<T> {
    var onChange: suspend () -> Unit = {}
    val autoRunner =
        CoAutoRunner(
            launcher = this,
            onChange = { onChange() },
            flowTransformer = flowTransformer,
            dispatcher = dispatcher,
            withLoading = withLoading,
            observer = observer,
        )
    val flow = callbackFlow {
        onChange = {
            send(autoRunner.run())
        }
        send(autoRunner.run())
        awaitClose { autoRunner.dispose() }
    }
    return flow.stateIn(launcherScope, started = started, initialValue = initial)
}

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * @param initial The initial value (until the first computation finishes).
 * @param launcher The [CoroutineLauncher] to use.
 * @param flowTransformer How changes should be collected. Defaults to `{ mapLatest { } }`.
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Whether loading state may be tracked for the (re-)computation. Defaults to `true`.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> CoroutineScope.derived(
    initial: T,
    started: SharingStarted,
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    flowTransformer: AutoRunFlowTransformer = { mapLatest { } },
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
