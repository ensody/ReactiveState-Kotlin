package com.ensody.reactivestate

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AutoRunnerStateFlowTest {
    @Test
    fun `autoRun with StateFlow`(): Unit = runBlockingTest {
        val source = MutableStateFlow(0)
        val target = MutableStateFlow(-1)
        val finished = CompletableDeferred<Unit>()
        val job = launch {
            val runner = autoRun { target.value = 2 * get(source) }

            // Right after creation of the AutoRunner the values should be in sync
            assertEquals(0, target.value)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertEquals(2 * it, target.value)
            }

            // Test disposing (target.value is out of sync)
            runner.dispose()
            val oldValue = target.value
            source.value += 5
            assertEquals(oldValue, target.value)

            // Re-enable AutoRunner
            runner.run()
            assertEquals(source.value * 2, target.value)
            source.value += 5
            assertEquals(source.value * 2, target.value)
            finished.complete(Unit)
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        finished.await()
        job.cancel()
        val oldValue = source.value * 2
        source.value += 5
        assertEquals(oldValue, target.value)
    }
}
