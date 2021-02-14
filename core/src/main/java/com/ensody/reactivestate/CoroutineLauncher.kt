package com.ensody.reactivestate

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** Interface for launching coroutines. */
public interface CoroutineLauncher {
    /** The underlying [CoroutineScope] of this launcher. */
    public val launcherScope: CoroutineScope

    /**
     * Launches a coroutine. Mark long-running coroutines by setting [withLoading] to `true`.
     *
     * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param withLoading Whether the loading state may be tracked for this coroutine. This should be false for
*                         long-running / never-terminating coroutines (e.g. when collecting a flow). Defaults to `true´.
     * @param onError Optional custom error handler.
     * @param block the coroutine code which will be invoked in the context of the provided scope.
     */
    public fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        withLoading: Boolean = true,
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job
}

/**
 * Simple default implementation of a [CoroutineLauncher] which uses a given [CoroutineScope].
 *
 * Usually you'll want to use a launcher which also does error handling and maybe even tracks a loading state.
 */
public class SimpleCoroutineLauncher(override val launcherScope: CoroutineScope) : CoroutineLauncher {
    override fun launch(
        context: CoroutineContext,
        start: CoroutineStart,
        withLoading: Boolean,
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit
    ): Job =
        launcherScope.launch(context = context, start = start) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError?.invoke(e) ?: throw e
            }
        }
}
