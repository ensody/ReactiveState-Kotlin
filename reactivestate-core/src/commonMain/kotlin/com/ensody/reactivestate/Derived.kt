package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private abstract class BaseDerivedStateFlow<T>(
    protected val launcher: CoroutineLauncher,
) : StateFlow<T> {

    protected var started = 0
    protected val mutex = Mutex()

    protected val onChangeFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    protected abstract val flow: Flow<T>
    protected abstract val autoRunner: InternalBaseAutoRunner

    override val replayCache: List<T> get() = listOf(value)

    init {
        onChangeFlow.tryEmit(Unit)
        launcher.invokeOnCompletion { autoRunner.dispose() }
    }

    protected fun Resolver.getValues(): List<Any?> = observables.values.map { it.value }
}

private class DerivedStateFlow<T>(
    launcher: CoroutineLauncher = scopelessCoroutineLauncher,
    private val observer: AutoRunCallback<T>,
) : BaseDerivedStateFlow<T>(launcher) {

    private val hasScope get() = launcher !== scopelessCoroutineLauncher

    override val autoRunner =
        AutoRunner(
            launcher = launcher,
            onChange = {
                if (started > 0) it.run()
                onChangeFlow.tryEmit(Unit)
            },
        ) {
            observer().also { cachedValue = Wrapped(it) }
        }
    override val flow = onChangeFlow.map { cachedValue.value }.distinctUntilChanged()

    /** The values of the observed dependencies. We only track this if nobody is subscribed. */
    private var dependencyValues: List<Any?>? = null

    /** Caches the last computed value. */
    private lateinit var cachedValue: Wrapped<T>

    override val value: T
        get() {
            if (started == 0 && autoRunner.resolver.getValues() != dependencyValues) {
                mutex.withSpinLock {
                    // The above subscriberCount check was the fast path without the lock
                    if (started == 0) {
                        val resolver = Resolver(autoRunner)
                        cachedValue = Wrapped(resolver.observer())
                        dependencyValues = resolver.getValues()
                        autoRunner.resolver = resolver
                    }
                }
            }
            return cachedValue.value
        }

    init {
        // If we have our own CoroutineScope we can start immediately. Otherwise everything is computed on-demand only.
        if (hasScope) {
            start()
        }
    }

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        try {
            mutex.withLock { start() }
            flow.collect(collector)
        } finally {
            mutex.withLock { stop() }
        }
        throw IllegalStateException("Should never get here")
    }

    private fun start() {
        started += 1
        if (started == 1) {
            autoRunner.run()
            dependencyValues = null
        }
    }

    protected fun stop() {
        if (--started == 0) {
            dependencyValues = autoRunner.resolver.getValues()
            autoRunner.dispose()
        }
    }
}

private class CoDerivedWhileSubscribedStateFlow<T>(
    initial: T,
    flowTransformer: DerivedFlowTransformer<T>,
    dispatcher: CoroutineDispatcher,
    private val withLoading: MutableValueFlow<Int>?,
    launcher: CoroutineLauncher = scopelessCoroutineLauncher,
    private val observer: CoAutoRunCallback<T>,
) : BaseDerivedStateFlow<T>(launcher) {

    override val autoRunner =
        CoAutoRunner(
            launcher = launcher,
            onChange = { onChangeFlow.tryEmit(Unit) },
            flowTransformer = Flow<Unit>::transform,
            dispatcher = dispatcher,
            withLoading = null,
        ) {
            observer().also { cachedValue = it }
        }
    override val flow = onChangeFlow.flowTransformer {
        launcher.track(withLoading = withLoading) {
            emit(autoRunner.run())
        }
    }.distinctUntilChanged()

    /** Caches the last computed value. */
    private var cachedValue: T = initial

    override val value: T get() = cachedValue

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        try {
            mutex.withLock { start() }
            flow.collect(collector)
        } finally {
            mutex.withLock { stop() }
        }
        throw IllegalStateException("Should never get here")
    }

    private suspend fun start() {
        started += 1
        if (started == 1) {
            autoRunner.run()
        }
    }

    protected fun stop() {
        if (--started == 0) {
            autoRunner.dispose()
        }
    }
}

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 *
 * This behaves like [SharingStarted.Eagerly] and computes the initial value by executing the [observer] function
 * immediately.
 *
 * This doesn't need a [CoroutineScope]/[CoroutineLauncher] and has [SharingStarted.WhileSubscribed] behavior.
 * If you have access to a [CoroutineScope]/[CoroutineLauncher] you should better use the normal [derived]
 * functions.
 */
public fun <T> derived(observer: AutoRunCallback<T>): StateFlow<T> =
    DerivedStateFlow(observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 *
 * This behaves like [SharingStarted.Eagerly] and computes the initial value by executing the [observer] function
 * immediately.
 */
public fun <T> CoroutineLauncher.derived(observer: AutoRunCallback<T>): StateFlow<T> =
    DerivedStateFlow(launcher = this, observer = observer)

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
 * This doesn't need a [CoroutineScope]/[CoroutineLauncher] and has [SharingStarted.WhileSubscribed] behavior.
 * If you have access to a [CoroutineScope]/[CoroutineLauncher] you should better use the normal [derived]
 * functions.
 *
 * @param initial The initial value (until the first computation finishes).
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to `null`.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> derivedWhileSubscribed(
    initial: T,
    flowTransformer: DerivedFlowTransformer<T> = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: MutableValueFlow<Int>? = null,
    observer: CoAutoRunCallback<T>,
): StateFlow<T> =
    CoDerivedWhileSubscribedStateFlow(
        initial = initial,
        flowTransformer = flowTransformer,
        dispatcher = dispatcher,
        withLoading = withLoading,
        observer = observer,
    )

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
    val onChangeFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
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
