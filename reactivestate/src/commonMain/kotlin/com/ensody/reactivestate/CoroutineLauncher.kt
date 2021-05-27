package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
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
        block: suspend CoroutineScope.() -> Unit
    ): Job
}
