package com.ensody.reactivestate

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

/**
 * This is used to send events to an observer. All events are queued for later processing.
 *
 * The possible events are defined as method calls on an interface [T]. This allows for easy composition of multiple
 * events. See [ErrorEvents] as an example:
 *
 * ```kotlin
 * interface MyHandlerEvents : ErrorEvents, OtherEvents {
 *     fun onSomethingHappened()
 * }
 *
 * class MyHandler {
 *     val eventNotifier = EventNotifier<MyHandlerEvents>()
 *
 *     fun doSomething() {
 *         withErrorHandling(eventNotifier) {
 *             if (computeResult() > 5) {
 *                 eventNotifier { onSomethingHappened() }
 *             } else {
 *                 eventNotifier { onOtherEvent() }
 *             }
 *         }
 *     }
 * }
 * ```
 */
public interface EventNotifier<T> : MutableFlow<suspend T.() -> Unit> {
    /** Adds a lambda function to the event stream. */
    public operator fun invoke(block: suspend T.() -> Unit)
}

/** Creates an [EventNotifier]. */
@Suppress("FunctionName")
public fun <T> EventNotifier(capacity: Int = Channel.UNLIMITED): EventNotifier<T> =
    EventNotifierImpl(capacity)

private class EventNotifierImpl<T>(capacity: Int = Channel.UNLIMITED) :
    EventNotifier<T>,
    MutableFlow<suspend T.() -> Unit> by MutableFlow(
        capacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ) {

    override operator fun invoke(block: suspend T.() -> Unit) {
        tryEmit(block)
    }
}
