package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private fun Map<Any, FrozenAutoRunnerObservable<*, *>>.getFrozenValues(): List<Any?> =
    values.map { it.revisionedValue }

private fun Map<Any, FrozenAutoRunnerObservable<*, *>>.getNewValues(): List<Any?> =
    values.map { it.observable.revisionedValue }

private fun <T> derivedCached(
    synchronous: Boolean,
    context: CoroutineContext,
    observer: AutoRunCallback<T>,
): StateFlow<T> {
    // The values of the observed dependencies. We only track this if nobody is subscribed.
    var prevObservables = mapOf<Any, FrozenAutoRunnerObservable<*, *>>()
    var dependencyValues: List<Any?>? = null
    var cachedValue: Wrapped<T>? = null
    val mutex = Mutex()

    fun getCached(onCached: (T) -> Unit = {}, block: () -> T): T {
        val cached = cachedValue
        return if (cached != null && prevObservables.getNewValues() == dependencyValues) {
            cached.value.also { onCached(it) }
        } else {
            block()
        }
    }

    @OptIn(InternalReactiveStateApi::class)
    return callbackFlow {
        autoRun {
            // We MUST NOT re-evaluate the observer lambda unnecessarily because it might be used to instantiate stable
            // objects like ViewModels
            val next = mutex.withSpinLock {
                getCached(
                    {
                        val prev = InternalResolver.run { prevObservables }
                        for ((underlying, observable) in prev) {
                            track(underlying) { observable.observable }.value
                        }
                    },
                ) {
                    observer()
                }
            }
            trySend(next)
            prevObservables = InternalResolver.run { observables }
            dependencyValues = prevObservables.getFrozenValues()
            cachedValue = Wrapped(next)
        }
        awaitClose {}
    }.stateOnDemand(synchronous = synchronous, emitValueOnStart = false) {
        getCached {
            mutex.withSpinLock {
                getCached {
                    runWithResolver {
                        observer().also {
                            dependencyValues = InternalResolver.run { observables.getFrozenValues() }
                            prevObservables = InternalResolver.run { observables }
                            cachedValue = Wrapped(it)
                        }
                    }
                }
            }
        }
    }
}

private fun <T> derivedOnDemand(
    synchronous: Boolean,
    context: CoroutineContext,
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    callbackFlow {
        autoRun { trySend(observer()) }
        awaitClose {}
    }.stateOnDemand(context = context, synchronous = synchronous, emitValueOnStart = false) {
        runWithResolver(observer)
    }

internal fun <T> scopelessDerived(
    context: CoroutineContext = EmptyCoroutineContext,
    synchronous: Boolean = true,
    cache: Boolean = true,
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    if (cache) {
        derivedCached(context = context, synchronous = synchronous, observer = observer)
    } else {
        derivedOnDemand(context = context, synchronous = synchronous, observer = observer)
    }

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 *
 * This variant doesn't need a [CoroutineScope]/[CoroutineLauncher].
 *
 * @param synchronous Whether `.value` access synchronously recomputes even if someone collects. Defaults to `true`.
 * @param cache Caching of [StateFlow.value] expensive computations while nobody collects. Defaults to `true`.
 */
public fun <T> derived(
    context: CoroutineContext = EmptyCoroutineContext,
    synchronous: Boolean = true,
    cache: Boolean = true,
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    scopelessDerived(context = context, synchronous = synchronous, cache = cache, observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 *
 * @param synchronous Whether `.value` access synchronously recomputes even if someone collects. Defaults to `true`.
 * @param cache Caching of [StateFlow.value] expensive computations while nobody collects. Defaults to `true`.
 */
public fun <T> CoroutineLauncher.derived(
    context: CoroutineContext = EmptyCoroutineContext,
    synchronous: Boolean = true,
    cache: Boolean = synchronous,
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    scopelessDerived(context = context, synchronous = synchronous, cache = cache, observer = observer).also {
        // Keep the value asynchronously updated in the background
        if (!synchronous) launch { it.collect {} }
    }

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via an [autoRun] block.
 *
 * This behaves like [SharingStarted.Eagerly] and computes the initial value by executing the [observer] function
 * immediately.
 */
public fun <T> CoroutineScope.derived(
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    synchronous: Boolean = true,
    cache: Boolean = synchronous,
    observer: AutoRunCallback<T>,
): StateFlow<T> =
    launcher.derived(synchronous = synchronous, cache = cache, observer = observer)

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * This doesn't need a [CoroutineScope]/[CoroutineLauncher] and has [SharingStarted.WhileSubscribed] behavior.
 * If you have access to a [CoroutineScope]/[CoroutineLauncher] you should better use the normal [derived]
 * functions.
 *
 * @param initial The initial value (until the first computation finishes).
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.main`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to `null`.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> derivedWhileSubscribed(
    initial: T,
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.main,
    withLoading: MutableStateFlow<Int>? = null,
    observer: CoAutoRunCallback<T>,
): StateFlow<T> {
    var value = initial
    return callbackFlow {
        coAutoRun(flowTransformer = flowTransformer, dispatcher = dispatcher, withLoading = withLoading) {
            val next = observer()
            trySend(next)
            value = next
        }
        awaitClose {}
    }.stateOnDemand(emitValueOnStart = false) {
        value
    }
}

/**
 * Creates a [StateFlow] that computes its value based on other [StateFlow]s via a suspendable [coAutoRun] block.
 *
 * You can use this to compute values on-demand only via [SharingStarted.WhileSubscribed].
 *
 * @param initial The initial value (until the first computation finishes).
 * @param started When the value should be updated. Pass [SharingStarted.WhileSubscribed] to compute only on demand.
 *                Defaults to [SharingStarted.Eagerly].
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.main`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.loading].
 * @param observer The callback which is used to track the observables.
 */
public fun <T> CoroutineLauncher.derived(
    initial: T,
    started: SharingStarted = SharingStarted.Eagerly,
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.main,
    withLoading: MutableStateFlow<Int>? = loading,
    observer: CoAutoRunCallback<T>,
): StateFlow<T> =
    callbackFlow {
        val runner = this@derived.coAutoRun(
            flowTransformer = flowTransformer,
            dispatcher = dispatcher,
            withLoading = withLoading,
        ) {
            trySend(observer())
        }
        awaitClose {
            runner.dispose()
        }
    }.stateIn(scope = scope, started = started, initialValue = initial)

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
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to `null`.
 * @param observer The callback which is used to track the observables.
 */
public fun <T> CoroutineScope.derived(
    initial: T,
    started: SharingStarted = SharingStarted.Eagerly,
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.main,
    withLoading: MutableStateFlow<Int>? = null,
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
