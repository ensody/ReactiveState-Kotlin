package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

internal class EventNotifierTest {
    @Test
    fun `EventNotifier buffers until subscriber consumes values`() = runBlockingTest {
        val flow = EventNotifier<MyView>()
        val collected = mutableListOf<Int>()
        val view = object : MyView {
            override fun add(x: Int) {
                collected.add(x)
            }
        }

        flow { add(0) }
        flow { add(1) }
        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf())

        var job = launch {
            flow.collect { view.it() }
        }

        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(0, 1))

        flow { add(2) }
        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(0, 1, 2))

        job.cancel()

        flow { add(3) }
        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(0, 1, 2))

        job = launch {
            flow.collect { view.it() }
        }

        advanceUntilIdle()
        assertThat(collected).isEqualTo(listOf(0, 1, 2, 3))
        job.cancel()
    }
}

private interface MyView {
    fun add(x: Int)
}
