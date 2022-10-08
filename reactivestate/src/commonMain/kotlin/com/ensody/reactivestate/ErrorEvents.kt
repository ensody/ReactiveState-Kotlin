package com.ensody.reactivestate

import kotlinx.coroutines.CancellationException

/** Events interface with a simple [onError] event (e.g. for use with [EventNotifier]). */
public interface ErrorEvents {
    /** Triggers an error event. */
    public fun onError(error: Throwable)
}

/** Executes the given [block], catching any errors and reporting them to the given [eventNotifier]. */
public suspend fun <E : ErrorEvents> withErrorReporting(
    eventNotifier: EventNotifier<E>,
    onError: (suspend (error: Throwable) -> Unit)?,
    block: suspend () -> Unit,
) {
    if (onError == null) {
        withErrorReporting(eventNotifier) { block() }
    } else {
        withErrorReporting(
            {
                withErrorReporting(eventNotifier) {
                    onError(it)
                }
            },
        ) { block() }
    }
}

/** Executes the given [block], catching any errors and reporting them to the given [eventNotifier]. */
public inline fun <E : ErrorEvents> withErrorReporting(eventNotifier: EventNotifier<E>, block: () -> Unit) {
    withErrorReporting(onError = { eventNotifier { onError(it) } }, block = block)
}

/** Executes the given [block], catching any errors and calling [onError], but handling [CancellationException]. */
public inline fun withErrorReporting(onError: (error: Throwable) -> Unit, block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        onError(e.throwIfFatal())
    }
}

/**
 * Consumes and handles [EventNotifier]'s events on the given [handler].
 *
 * Any errors during event handling will trigger [ErrorEvents.onError] on the [handler].
 */
public suspend fun <T : ErrorEvents> EventNotifier<T>.handleEvents(handler: T) {
    collect {
        withErrorReporting(handler::onError) {
            handler.it()
        }
    }
}
