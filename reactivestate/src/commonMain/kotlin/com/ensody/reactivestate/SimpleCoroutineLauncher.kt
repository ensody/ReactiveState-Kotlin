package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Simple default implementation of a [CoroutineLauncher] which uses a given [CoroutineScope].
 *
 * Usually you'll want to use a [ReactiveState] which also does error handling.
 */
public open class SimpleCoroutineLauncher(final override val launcherScope: CoroutineScope) : CoroutineLauncher {
    final override val isAnyLoading: LoadingStateTracker by lazy { LoadingStateTracker(launcherScope) }
    final override val generalLoading: MutableValueFlow<Int> by lazy { isAnyLoading.createLoadingState() }

    protected open fun rawLaunch(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> Unit,
    ): Job =
        launcherScope.launch(context = context, start = start, block = block)

    override fun launch(
        context: CoroutineContext,
        start: CoroutineStart,
        withLoading: MutableValueFlow<Int>?,
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit,
    ): Job =
        rawLaunch(context = context, start = start) {
            withErrorReporting({ onError?.invoke(it) ?: onError(it) }) {
                try {
                    withLoading?.increment()
                    block()
                } finally {
                    withLoading?.decrement()
                }
            }
        }

    public open fun onError(error: Throwable) {
        throw error
    }
}
