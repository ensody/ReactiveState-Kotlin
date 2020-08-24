package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Default work queue entry type. */
public typealias WorkQueueEntry = suspend () -> Unit

/** Callback used to configure the Flow of a [WorkQueue]. */
public typealias WorkQueueConfigCallback<T> = Flow<T>.() -> Flow<*>

/** Executes each simple lambda in a [Flow]. */
public fun Flow<WorkQueueEntry>.worker(): Flow<Unit> = map { it() }

/** Executes each simple lambda in a conflated [Flow] using [conflatedMap]. */
public fun Flow<WorkQueueEntry>.conflatedWorker(timeoutMillis: Long = 0): Flow<Unit> =
    conflatedMap(timeoutMillis) { it() }

/** Maps a conflated [Flow] with [timeoutMillis] delay between the first and last element. */
public inline fun <T, R> Flow<T>.conflatedMap(
    timeoutMillis: Long = 0,
    crossinline transform: suspend (value: T) -> R,
): Flow<R> = conflate().map(transform).addDelay(timeoutMillis)

/** Transforms a conflated [Flow] with [timeoutMillis] delay between the first and last element. */
public inline fun <T, R> Flow<T>.conflatedTransform(
    timeoutMillis: Long = 0,
    crossinline transform: suspend FlowCollector<R>.(value: T) -> Unit,
): Flow<R> = conflate().transform(transform).addDelay(timeoutMillis)

/** Adds a [timeoutMillis] delay to a [Flow]. If delay is zero or negative this is a no-op. */
public fun <T> Flow<T>.addDelay(timeoutMillis: Long): Flow<T> {
    require(timeoutMillis >= 0) { "Timeout should be greater than or equal to 0" }
    return if (timeoutMillis == 0L) this else onEach { delay(timeoutMillis) }
}

/** Creates a [WorkQueue]. You have to manually call `consume()`. */
public fun <T> CoroutineScope.workQueue(): WorkQueue<T> = WorkQueue(this)

/** Creates a [WorkQueue]. You have to manually call `consume()`. */
public fun <T> Scoped.workQueue(): WorkQueue<T> = scope.workQueue()

/** Creates a [WorkQueue] for lambdas taking an argument. You have to manually call `consume()`. */
public fun <T> CoroutineScope.argWorkQueue(): WorkQueue<suspend (T) -> Unit> = WorkQueue(this)

/** Creates a [WorkQueue] for lambdas taking an argument. You have to manually call `consume()`. */
public fun <T> Scoped.argWorkQueue(): WorkQueue<suspend (T) -> Unit> = scope.argWorkQueue()

/** Creates a [WorkQueue] for lambdas taking a `this` argument. You have to manually call `consume()`. */
public fun <T> CoroutineScope.thisWorkQueue(): WorkQueue<suspend T.() -> Unit> = WorkQueue(this)

/** Creates a [WorkQueue] for lambdas taking a `this` argument. You have to manually call `consume()`. */
public fun <T> Scoped.thisWorkQueue(): WorkQueue<suspend T.() -> Unit> = scope.thisWorkQueue()

/** Creates a [WorkQueue] and starts consuming it with the given [config]. */
public fun <T> CoroutineScope.workQueue(workers: Int = 1, config: WorkQueueConfigCallback<T>): WorkQueue<T> =
    workQueue<T>().apply { consume(this@workQueue, workers = workers, config = config) }

/** Creates a [WorkQueue] and starts consuming it with the given [config]. */
public fun <T> Scoped.workQueue(workers: Int = 1, config: WorkQueueConfigCallback<T>): WorkQueue<T> =
    workQueue<T>().apply { consume(scope, workers = workers, config = config) }

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [worker]. */
public fun CoroutineScope.simpleWorkQueue(workers: Int = 1): WorkQueue<suspend () -> Unit> =
    workQueue(workers = workers) { worker() }

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [worker]. */
public fun Scoped.simpleWorkQueue(workers: Int = 1): WorkQueue<suspend () -> Unit> =
    workQueue(workers = workers) { worker() }

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [conflatedWorker]. */
public fun CoroutineScope.conflatedWorkQueue(timeoutMillis: Long = 0L): WorkQueue<suspend () -> Unit> =
    workQueue { conflatedWorker(timeoutMillis) }

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [conflatedWorker]. */
public fun Scoped.conflatedWorkQueue(timeoutMillis: Long = 0L): WorkQueue<suspend () -> Unit> =
    workQueue { conflatedWorker(timeoutMillis) }

/**
 * Executes tasks, allowing to conflate/debounce/sample/etc. the execution.
 *
 * To start executing tasks you have to call [consume].
 *
 * This is useful for launching e.g. click handlers from a Fragment, but executing them within
 * viewModelScope, so requests won't be aborted when rotating the screen.
 *
 * You can also use this to let the Fragment consume result events. In this case, the Fragment
 * would call `consume(lifecycleScope) { ... }`, so the consumption is bound to the Fragment's
 * lifecycle. Only use this pattern for communicating events. Usually you'll communicate results
 * as state using a `LiveData`.
 *
 * @param [scope] The queue only lives as long as the given scope's context is active.
 */
public class WorkQueue<T>(private val scope: CoroutineScope) : Disposable {
    private val channel = Channel<T>(BUFFERED)
    private val selfDisposer = disposeOnCompletionOf(scope)
    private val consumersDisposer = DisposableGroup()

    /** Adds a new entry to the work queue. Can be called outside of a coroutine. */
    public fun launch(entry: T) {
        scope.launch {
            add(entry)
        }
    }

    /** Adds a new entry to the work queue. Must be called from a coroutine. */
    public suspend fun add(entry: T) {
        channel.send(entry)
    }

    /**
     * Starts consuming the queue's [Channel] as a [Flow].
     *
     * Disposes any existing consumers, so the last call to consume() overrides any previous calls.
     *
     * You can configure the [Flow] using a helper like [conflatedWorker] or [conflatedMap] or
     * custom code like `{ conflate().map { it(); delay(200 } }`.
     * Note that custom code must execute each element in the flow, for instance with
     * `.map { it(); ... }`.
     *
     * IMPORTANT: Don't call this method in parallel from multiple threads or coroutines.
     *
     * @param [scope] Consumers' [CoroutineScope].
     * @param [workers] Number of worker coroutines.
     * @param [config] Configures the [Flow].
     */
    public fun consume(
        scope: CoroutineScope,
        workers: Int = 1,
        config: WorkQueueConfigCallback<T>,
    ): Disposable {
        require(workers > 0) { "WorkQueue.consume(): Number of workers must be positive" }
        consumersDisposer.dispose()
        repeat(workers) {
            consumersDisposer.add(scope.launch {
                config(channel.receiveAsFlow()).collect()
            })
        }
        consumersDisposer.add(consumersDisposer.disposeOnCompletionOf(scope))
        return consumersDisposer
    }

    /**
     * Disposes the queue.
     *
     * This is called automatically on [scope] completion.
     *
     * Note: In unit tests you have to explicitly call this by hand or cancel the scope.
     */
    override fun dispose() {
        selfDisposer.dispose()
        channel.cancel()
        consumersDisposer.dispose()
    }
}

/** Consume work queue, passing [arg] to each lambda. */
public fun <T> WorkQueue<suspend (T) -> Unit>.consume(
    arg: T,
    scope: CoroutineScope,
    workers: Int = 1,
): Disposable = consume(scope, workers = workers) { map { it(arg) } }

/** Consume work queue, conflate lambdas and pass [arg] to each lambda. */
public fun <T> WorkQueue<suspend (T) -> Unit>.conflatedConsume(
    arg: T,
    scope: CoroutineScope,
    timeoutMillis: Long = 0,
    workers: Int = 1,
): Disposable = consume(scope, workers = workers) { conflatedMap(timeoutMillis) { it(arg) } }
