package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * An interface for ViewModels and any other objects that can trigger one-time events/actions and handle errors.
 *
 * Make sure you always launch coroutines via [launch] (instead of the scope) to get automatic error handling.
 *
 * @see [BaseReactiveState] for a ready-made base class (or delegate).
 */
public interface ReactiveState<T : ErrorEvents> : CoroutineLauncher {
    public val eventNotifier: EventNotifier<T>
}

/**
 * Base class/delegate for ViewModels and other objects that can trigger one-time events/actions and handle errors.
 *
 * Make sure you always launch coroutines via [launch] (instead of the scope) to get automatic error handling.
 */
public open class BaseReactiveState<T : ErrorEvents>(scope: CoroutineScope) :
    ReactiveState<T>, SimpleCoroutineLauncher(scope) {

    override val eventNotifier: EventNotifier<T> = EventNotifier()

    override fun launch(
        context: CoroutineContext,
        start: CoroutineStart,
        withLoading: MutableValueFlow<Int>?,
        onError: (suspend (Throwable) -> Unit)?,
        block: suspend CoroutineScope.() -> Unit,
    ): Job =
        launcherScope.launch(context = context, start = start) {
            withErrorReporting(eventNotifier, { onError?.invoke(it) ?: throw it }) {
                try {
                    withLoading?.increment()
                    block()
                } finally {
                    withLoading?.decrement()
                }
            }
        }
}
