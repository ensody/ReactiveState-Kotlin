package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class WorkQueueTest {
    @Test
    fun conflatedResultQueue() = runBlockingTest {
        val queue = simpleWorkQueue()
        val resultQueue = workQueue<(MutableList<Int>) -> Unit>()

        // Ensure only the last consume() call wins
        val inactiveResults = mutableListOf<Int>()
        resultQueue.consume(this) { conflatedMap(200) { it(inactiveResults) } }
        val results = mutableListOf<Int>()
        resultQueue.consume(this) { conflatedMap(200) { it(results) } }

        queue.launch { resultQueue.add { it.add(1) } }
        assertThat(results).isEqualTo(listOf(1))
        queue.launch { resultQueue.add { it.add(2) } }
        queue.launch { resultQueue.add { it.add(3) } }
        queue.launch { resultQueue.add { it.add(4) } }
        advanceUntilIdle()
        assertThat(results).isEqualTo(listOf(1, 4))
        assertThat(inactiveResults).isEmpty()

        queue.dispose()
        resultQueue.dispose()
    }

    @Test
    fun conflatedQueue() = runBlockingTest {
        val queue = conflatedWorkQueue(200)
        val results = mutableListOf<Int>()
        queue.launch { results.add(1) }
        queue.launch { results.add(2) }
        queue.launch { results.add(3) }
        queue.launch { results.add(4) }
        advanceUntilIdle()
        assertThat(results).isEqualTo(listOf(1, 4))
        queue.dispose()
    }

    @Test
    fun autoDispose() = runBlockingTest {
        val results = mutableListOf<Int>()
        launch {
            val queue = conflatedWorkQueue(200)
            queue.launch { results.add(1) }
            queue.launch { results.add(2) }
            queue.launch { results.add(3) }
            queue.launch { results.add(4) }
            cancel()
        }
        advanceUntilIdle()
        assertThat(results).isEqualTo(listOf(1))
    }
}
