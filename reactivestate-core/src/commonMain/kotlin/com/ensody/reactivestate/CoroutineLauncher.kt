package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Interface for launching coroutines with error handling and loading state tracking.
 *
 * You can track multiple different kinds of loading states by defining separate [MutableValueFlow].
 *
 * @see [ReactiveState] for a full implementation that you'll usually want to use.
 */
public interface CoroutineLauncher {
    /** The underlying [CoroutineScope] of this launcher. */
    public val launcherScope: CoroutineScope

    /**
     * The default loading tracker.
     *
     * Use [increment]/[decrement] to safely update the loading counter.
     */
    public val loading: MutableValueFlow<Int>

    /**
     * Launches a coroutine without any error handling or loading state tracking.
     *
     * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param block the coroutine code which will be invoked in the context of the provided scope.
     */
    public fun rawLaunch(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> Unit,
    ): Job =
        launcherScope.launch(context = context, start = start, block = block)

    /**
     * Launches a coroutine. Mark long-running coroutines by setting [withLoading] to loading state.
     *
     * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param withLoading Tracks loading state for the (re-)computation. Defaults to [loading].
     *                    This should be `null` for long-running / never-terminating coroutines (e.g. `flow.collect`).
     * @param onError Optional custom error handler.
     * @param block the coroutine code which will be invoked in the context of the provided scope.
     */
    public fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        withLoading: MutableValueFlow<Int>? = loading,
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job =
        rawLaunch(context = context, start = start) {
            track(withLoading = withLoading, onError = onError) {
                block()
            }
        }

    /**
     * Tracks a suspension [block]'s loading state and errors.
     *
     * Mark long-running coroutines by setting [withLoading] to loading state.
     *
     * @param withLoading Tracks loading state for the (re-)computation. Defaults to [loading].
     *                    This should be `null` for long-running / never-terminating coroutines (e.g. `flow.collect`).
     * @param onError Optional custom error handler.
     * @param block the coroutine code which will be invoked in the context of the provided scope.
     */
    public suspend fun track(
        withLoading: MutableValueFlow<Int>? = loading,
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend () -> Unit,
    ) {
        withErrorReporting({ onError?.invoke(it) ?: onError(it) }) {
            try {
                withLoading?.increment()
                block()
            } finally {
                withLoading?.decrement()
            }
        }
    }

    public fun onError(error: Throwable) {
        throw error
    }
}
