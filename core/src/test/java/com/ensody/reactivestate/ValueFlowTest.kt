package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

internal class ValueFlowTest {
    @Test
    fun `ValueFlow emits first value`() = runBlockingTest {
        val flow = MutableValueFlow(mutableListOf(0))
        assertThat(flow.first().first()).isEqualTo(0)
        flow.update { it[0] = 1 }
        assertThat(flow.first().first()).isEqualTo(1)
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
        assertThat(collected).isEqualTo(listOf(0, 1))
        flow.value = 1
        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(0, 1))

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
        assertThat(collected).isEqualTo(listOf(0))
        flow.update { }
        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(0, 0))
        flow.value = 1
        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(0, 0, 1))

        job.cancel()

        job = launch {
            flow.collect { collected.add(it) }
        }

        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(0, 0, 1, 1))

        job.cancel()
    }
}
