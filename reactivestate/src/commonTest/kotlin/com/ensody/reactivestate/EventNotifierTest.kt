package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EventNotifierTest : CoroutineTest() {
    @Test
    fun eventNotifierBuffersUntilSubscriberConsumesValues() = runTest {
        val eventNotifier = EventNotifier<MyEvents>()
        val collected = mutableListOf<Int>()
        val eventHandler = object : MyEvents {
            override fun add(x: Int) {
                collected.add(x)
            }
        }

        eventNotifier { add(0) }
        eventNotifier { add(1) }
        advanceUntilIdle()
        assertEquals(listOf(), collected)

        var job = launch {
            eventNotifier.collect { eventHandler.it() }
        }

        advanceUntilIdle()
        assertEquals(listOf(0, 1), collected)

        eventNotifier { add(2) }
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 2), collected)

        job.cancel()

        eventNotifier { add(3) }
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 2), collected)

        job = launch {
            eventNotifier.collect { eventHandler.it() }
        }

        advanceUntilIdle()
        assertEquals(listOf(0, 1, 2, 3), collected)
        job.cancel()
    }

    @Test
    fun withErrorReportingSendsErrorEvents() = runTest {
        val eventNotifier = EventNotifier<ErrorEvents>()
        val error = IllegalStateException()
        withErrorReporting(eventNotifier) {
            throw error
        }
        val observer = MockErrorEvents()
        val event = eventNotifier.first()
        observer.event()
        assertEquals(listOf<Throwable>(error), observer.onErrorCalls)
    }
}

private interface MyEvents {
    fun add(x: Int)
}
