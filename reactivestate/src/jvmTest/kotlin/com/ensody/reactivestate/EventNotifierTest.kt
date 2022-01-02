package com.ensody.reactivestate

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EventNotifierTest {
    @Test
    fun eventNotifierBuffersUntilSubscriberConsumesValues() = runBlockingTest {
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
    fun withErrorReportingSendsErrorEvents() = runBlockingTest {
        val eventNotifier = EventNotifier<ErrorEvents>()
        val error = IllegalStateException()
        withErrorReporting(eventNotifier) {
            throw error
        }
        val observer: ErrorEvents = mockk(relaxed = true)
        val event = eventNotifier.first()
        observer.event()
        verify { observer.onError(error) }
    }
}

private interface MyEvents {
    fun add(x: Int)
}
