package com.ensody.reactivestate.test

import com.ensody.reactivestate.EventNotifier
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
public abstract class EventNotifierTest<T> : CoroutineTest() {
    /** Whether tests */
    public open val handleEventsInRunBlockingTest: Boolean = true
    public abstract val eventNotifier: EventNotifier<T>
    public abstract val events: T

    public fun handleEvents() {
        coroutineTestRule.testCoroutineScope.launch {
            eventNotifier.collect { events.it() }
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
