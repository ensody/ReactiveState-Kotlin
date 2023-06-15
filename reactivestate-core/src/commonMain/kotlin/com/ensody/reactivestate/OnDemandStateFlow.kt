package com.ensody.reactivestate

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Creates a computed [StateFlow] without requiring a [CoroutineScope] (unlike [stateIn]).
 *
 * This can be useful for callback based observers where you only want to register the [observer] when this [StateFlow]
 * gets collected. When nobody collects, the [StateFlow.value] gets computed via [getter].
 *
 * The [observer] receives an [OnDemandStateFlowContext] as `this` and should keep the [OnDemandStateFlowContext.value]
 * constantly updated. You can either use `try`-`finally` or [OnDemandStateFlowContext.invokeOnCancellation] to clean up
 * when all collectors have stopped.
 */
public fun <T> OnDemandStateFlow(
    getter: MutableStateFlow<T>?.() -> T,
    context: CoroutineContext = EmptyCoroutineContext,
    observer: suspend OnDemandStateFlowContext<T>.() -> Unit,
): StateFlow<T> =
    DefaultOnDemandStateFlow(
        getter = getter,
        observer = observer,
        delegate = MutableStateFlow(null.getter()),
        context = context,
    )

/**
 * Passed to [OnDemandStateFlow]'s `observer` which must keep [value] constantly updated.
 *
 * You can either use `try`-`finally` or [OnDemandStateFlowContext.invokeOnCancellation] to clean up when
 * all collectors have stopped.
 */
public class OnDemandStateFlowContext<T>(private val delegate: MutableStateFlow<T>) {
    public var value: T
        get() = delegate.value
        set(value) { delegate.value = value }

    internal var cancellationBlock: (suspend () -> Unit)? = null

    public suspend fun awaitCancellation() {
        CompletableDeferred<Unit>().await()
    }

    public fun invokeOnCancellation(block: suspend () -> Unit) {
        cancellationBlock = block
    }
}

private class DefaultOnDemandStateFlow<T>(
    private val getter: MutableStateFlow<T>?.() -> T,
    private val observer: suspend OnDemandStateFlowContext<T>.() -> Unit,
    private val delegate: MutableStateFlow<T>,
    private val context: CoroutineContext = EmptyCoroutineContext,
) : StateFlow<T> by delegate {

    private val mutex = Mutex()
    private var job: Job? = null
    private var subscriptionCount = 0

    override val value: T get() = if (subscriptionCount == 0) delegate.getter() else delegate.value

    private fun flowContext() = OnDemandStateFlowContext(delegate)

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        if (delegate.subscriptionCount.value == 0) {
            delegate.value = delegate.getter()
        }
        mutex.withLock {
            subscriptionCount += 1
            if (job == null) {
                job = mainScope.launch(context = context) {
                    val flowContext = flowContext()
                    try {
                        flowContext.observer()
                    } finally {
                        withContext(NonCancellable) {
                            flowContext.cancellationBlock?.invoke()
                        }
                    }
                }
            }
        }
        try {
            delegate.collect(collector)
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    subscriptionCount -= 1
                    if (subscriptionCount == 0) {
                        job?.cancel()
                        job = null
                    }
                }
            }
        }
    }
}
