package com.ensody.reactivestate

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

/**
 * This is used to send events to an observer.
 *
 * The possible events are defined as method calls on an interface [T].
 *
 * In case of overflow, this will drop the oldest entry by default (e.g. if you have 100s of events per second that
 * aren't processed by the view quickly enough).
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
