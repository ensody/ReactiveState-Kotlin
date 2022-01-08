package com.ensody.reactivestate.test

import com.ensody.reactivestate.EventNotifier
import com.ensody.reactivestate.throwIfFatal
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope

/**
 * Base class for unit testing an [EventNotifier].
 *
 * You have to override the [eventNotifier] and [events] attributes. Usually, [events] will be a mock.
 *
 * By default this handles events in [runBlockingTest]. You can disable this by overriding
 * [handleEventsInRunBlockingTest] to `false`. In that case you have to explicitly call [handleEvents] in each test.
 */
public abstract class EventNotifierTest<E> : CoroutineTest() {
    public open val handleEventsInRunBlockingTest: Boolean = true
    public abstract val eventNotifier: EventNotifier<E>
    public abstract val events: E

    public fun handleEvents() {
        testCoroutineScope.launch {
            try {
                eventNotifier.collect { events.it() }
            } catch (e: Throwable) {
                e.throwIfFatal().printStackTrace()
                throw e
            }
        }
    }

    override fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) {
        super.runBlockingTest {
            if (handleEventsInRunBlockingTest) {
                handleEvents()
                advanceUntilIdle()
            }
            block()
        }
    }
}
