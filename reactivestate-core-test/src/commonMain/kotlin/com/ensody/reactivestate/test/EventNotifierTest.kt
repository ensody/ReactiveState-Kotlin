package com.ensody.reactivestate.test

import com.ensody.reactivestate.EventNotifier
import com.ensody.reactivestate.throwIfFatal
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope

/**
 * Base class for unit testing an [EventNotifier].
 *
 * You have to override the [eventNotifier] and [events] attributes. Usually, [events] will be a mock.
 *
 * By default this handles events in [runTest]. You can disable this by overriding
 * [handleEventsInRunTest] to `false`. In that case you have to explicitly call [handleEvents] in each test.
 */
public abstract class EventNotifierTest<E> : CoroutineTest() {

    public open val handleEventsInRunTest: Boolean = true

    /** The dispatcher to use for [handleEvents]. */
    public open val eventsDispatcher: TestDispatcher = testDispatcher
    public abstract val eventNotifier: EventNotifier<E>
    public abstract val events: E

    public fun handleEvents() {
        val job = testScope.launch(eventsDispatcher) {
            try {
                eventNotifier.collect { events.it() }
            } catch (e: Throwable) {
                e.throwIfFatal().printStackTrace()
                throw e
            }
        }
        attachedDisposables.add(job)
    }

    override fun runTest(block: suspend TestScope.() -> Unit): TestResult =
        super.runTest {
            if (handleEventsInRunTest) {
                handleEvents()
            }
            block()
        }
}
