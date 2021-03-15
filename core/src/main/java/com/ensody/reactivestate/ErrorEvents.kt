package com.ensody.reactivestate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

/** Events interface with a simple [onError] event (e.g. for use with [EventNotifier]). */
public interface ErrorEvents {
    /** Triggers an error event. */
    public fun onError(error: Throwable)
}

/** Executes the given [block], catching any errors and reporting them to the given [eventNotifier]. */
public suspend fun <E : ErrorEvents> withErrorReporting(
    eventNotifier: EventNotifier<E>,
    onError: (suspend (error: Throwable) -> Unit)?,
    block: () -> Unit,
) {
    if (onError == null) {
        withErrorReporting(eventNotifier, block = block)
    } else {
        withErrorReporting({ onError(it) }, block = block)
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
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        onError(e)
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
