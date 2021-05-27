package com.ensody.reactivestate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

internal class ValueFlowTest {
    @Test
    fun `replaceLocked replaces the value`() = runBlockingTest {
        val flow = MutableValueFlow(0)
        flow.replace { this + 1 }
        assertEquals(1, flow.first())
        assertEquals(1, flow.replaceLocked { this + 1 })
        assertEquals(2, flow.first())
        flow.increment()
        assertEquals(3, flow.first())
        flow.decrement()
        assertEquals(2, flow.first())
    }

    @Test
    fun `increment and decrement replace the value`() = runBlockingTest {
        val flow = MutableValueFlow(0)
        flow.increment()
        assertEquals(1, flow.first())
        flow.decrement()
        assertEquals(0, flow.first())
    }

    @Test
    fun `ValueFlow emits first value`() = runBlockingTest {
        val flow = MutableValueFlow(mutableListOf(0))
        assertEquals(0, flow.first().first())
        flow.update { it[0] = 1 }
        assertEquals(1, flow.first().first())
    }

    @Test
    fun `value assignment behaves like MutableStateFlow`() = runBlockingTest {
        val initial = SomeData("")
        val stateFlow = MutableStateFlow(initial)
        val valueFlow = MutableValueFlow(initial)

        // Assign a new value
        val newValue = initial.copy()
        stateFlow.value = newValue
        valueFlow.value = newValue

        assertNotSame(newValue, stateFlow.value)
        assertSame(initial, stateFlow.value)
        assertSame(stateFlow.value, valueFlow.value)
    }

    @Test
    fun `ValueFlow distinctUntilChanged behavior when assigning to value`() = runBlockingTest {
        val flow = MutableValueFlow(0)
        val collected = mutableListOf<Int>()
        var job = launch {
            flow.collect { collected.add(it) }
        }
        advanceUntilIdle()

        flow.value = 1
        advanceUntilIdle()
        assertEquals(listOf(0, 1), collected)
        flow.value = 1
        advanceUntilIdle()
        assertEquals(listOf(0, 1), collected)

        job.cancel()
    }

    @Test
    fun `ValueFlow collect emits values continuously and without equality check`() = runBlockingTest {
        val flow = MutableValueFlow(0)
        val collected = mutableListOf<Int>()

        var job = launch {
            flow.collect { collected.add(it) }
        }

        advanceUntilIdle()
        assertEquals(listOf(0), collected)
        flow.update { }
        advanceUntilIdle()
        assertEquals(listOf(0, 0), collected)
        flow.value = 1
        advanceUntilIdle()
        assertEquals(listOf(0, 0, 1), collected)

        job.cancel()

        job = launch {
            flow.collect { collected.add(it) }
        }

        advanceUntilIdle()
        assertEquals(listOf(0, 0, 1, 1), collected)

        job.cancel()
    }
}
