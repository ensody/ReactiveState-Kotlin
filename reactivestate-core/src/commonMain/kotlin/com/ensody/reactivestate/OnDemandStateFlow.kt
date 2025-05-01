package com.ensody.reactivestate

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Turns this [Flow] into a [StateFlow] without requiring a [CoroutineScope] (unlike [stateIn]).
 *
 * When the resulting [StateFlow] gets collected this in turn starts collecting this underlying [Flow].
 * When nobody collects, the [StateFlow.value] is [initial] or the last collected value.
 *
 * When nobody collects, this is safe for garbage collection.
 *
 * Normally you'd use this together with [callbackFlow], for example.
 */
public fun <T> Flow<T>.stateOnDemand(
    initial: T,
    context: CoroutineContext = EmptyCoroutineContext,
    synchronous: Boolean = true,
    emitValueOnStart: Boolean = true,
): StateFlow<T> =
    stateOnDemand(context = context, synchronous = synchronous, emitValueOnStart = emitValueOnStart) {
        if (it == null) initial else it.value
    }

/**
 * Turns this [Flow] into a [StateFlow] without requiring a [CoroutineScope] (unlike [stateIn]).
 *
 * When the resulting [StateFlow] gets collected this in turn starts collecting this underlying [Flow].
 * When nobody collects, the [StateFlow.value] gets computed via [getter] which is also passed the previous value
 * wrapped in a [Wrapped] instance or null if there is no previous value, yet.
 * This way you can implement simple caching.
 *
 * When nobody collects, this is safe for garbage collection.
 *
 * Normally you'd use this together with [callbackFlow], for example.
 */
public fun <T> Flow<T>.stateOnDemand(
    context: CoroutineContext = EmptyCoroutineContext,
    synchronous: Boolean = true,
    emitValueOnStart: Boolean = true,
    getter: (previous: Wrapped<T>?) -> T,
): StateFlow<T> =
    DefaultOnDemandStateFlow(this, context, synchronous, emitValueOnStart, getter)

/**
 * Turns this [Flow] into a [SharedFlow] without requiring a [CoroutineScope] (unlike [shareIn]).
 *
 * The underlying [Flow] is only collected while there is at least one collector.
 * When nobody collects, this is safe for garbage collection.
 *
 * Normally you'd use this together with [callbackFlow], for example.
 * Alternatively you can construct a [SharedFlow] directly via [sharedFlow].
 */
public fun <T> Flow<T>.shareOnDemand(
    replay: Int = 0,
    context: CoroutineContext = EmptyCoroutineContext,
): SharedFlow<T> =
    internalShareOnDemand(replay = replay, context = context)

/**
 * Creates a computed [SharedFlow] without requiring a [CoroutineScope] (unlike [shareIn]).
 *
 * The [observer] block is only executed while there is at least one collector.
 * When nobody collects, this is safe for garbage collection.
 */
public inline fun <T> sharedFlow(
    replay: Int = 0,
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline observer: suspend ProducerScope<T>.() -> Unit,
): SharedFlow<T> =
    callbackFlow { observer() }.shareOnDemand(replay = replay, context = context)

internal fun <T> Flow<T>.internalShareOnDemand(
    replay: Int = 0,
    context: CoroutineContext = EmptyCoroutineContext,
): DefaultOnDemandSharedFlow<T> =
    if (this is DefaultOnDemandSharedFlow<T> && this.replay == replay && this.context == context) {
        this
    } else {
        DefaultOnDemandSharedFlow(this, replay, context, MutableSharedFlow(replay = replay))
    }

internal class SharedCollect<T>(
    val flow: Flow<T>,
    val context: CoroutineContext,
    val delegate: MutableSharedFlow<T>,
    val awaitFirstEmit: Boolean = false,
) : SharedFlow<T> by delegate {

    private val mutex = Mutex()
    private var job: Job? = null
    val subscriptionCount = MutableStateFlow(0)

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        val hasFirstEmit = CompletableDeferred<Unit>()
        mutex.withLock {
            subscriptionCount.value += 1
            if (job == null) {
                job = mainScope.launch(context = context) {
                    flow.collect {
                        delegate.emit(it)
                        hasFirstEmit.complete(Unit)
                    }
                }
            } else {
                hasFirstEmit.complete(Unit)
            }
        }
        try {
            if (awaitFirstEmit) hasFirstEmit.await()
            delegate.collect(collector)
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    subscriptionCount.value -= 1
                    if (subscriptionCount.value == 0) {
                        job?.cancel()
                        job = null
                    }
                }
            }
        }
    }
}

internal class DefaultOnDemandSharedFlow<T>(
    val flow: Flow<T>,
    val replay: Int,
    val context: CoroutineContext,
    val delegate: MutableSharedFlow<T>,
) : SharedFlow<T> by SharedCollect(flow, context, delegate)

internal class DefaultOnDemandStateFlow<T>(
    val flow: Flow<T>,
    val context: CoroutineContext,
    val synchronous: Boolean,
    val emitValueOnStart: Boolean,
    val getter: (previous: Wrapped<T>?) -> T,
) : StateFlow<T> {

    private val delegate = MutableStateFlow<Any?>(NIL)
    private val sharedCollect = SharedCollect(flow, context, delegate, awaitFirstEmit = true)

    override val replayCache: List<T> get() = listOf(value)

    override val value: T
        get() =
            if (synchronous || delegate.subscriptionCount.value == 0 || delegate.value === NIL) {
                @Suppress("UNCHECKED_CAST")
                val previous = if (delegate.value === NIL) null else Wrapped(delegate.value as T)
                getter(previous).also {
                    if (previous == null || it !== previous.value) {
                        delegate.value = it
                    }
                }
            } else {
                @Suppress("UNCHECKED_CAST")
                delegate.value as T
            }

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        // Emit current value
        var initialValue: Any? = if (emitValueOnStart) {
            value.also { collector.emit(it) }
        } else {
            NIL
        }
        sharedCollect.collect {
            @Suppress("UNCHECKED_CAST")
            if (it !== NIL && (initialValue === NIL || it != initialValue)) collector.emit(it as T)
            initialValue = NIL
        }
    }
}

internal val NIL = object {}
