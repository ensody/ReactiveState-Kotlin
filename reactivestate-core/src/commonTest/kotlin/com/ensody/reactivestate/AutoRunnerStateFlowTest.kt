package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AutoRunnerStateFlowTest : CoroutineTest() {
    @Test
    fun autoRunWithStateFlow() = runTest {
        val source = MutableStateFlow(0)
        val target = MutableStateFlow(-1)
        val cotarget = MutableStateFlow(-1)
        val finished = CompletableDeferred<Unit>()
        val job = launch {
            val runner = autoRun { target.value = 2 * get(source) }
            val corunner = coAutoRun { cotarget.value = 2 * get(source) }
            assertTrue(runner.isActive)

            // Right after creation of the AutoRunner the values should be in sync
            assertEquals(0, target.value)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                runCurrent()
                assertEquals(2 * it, target.value)
                assertEquals(2 * it, cotarget.value)
            }

            // Test disposing (target.value is out of sync)
            runner.dispose()
            corunner.dispose()
            val oldValue = target.value
            source.value += 5
            runCurrent()
            assertEquals(oldValue, target.value)
            assertEquals(oldValue, cotarget.value)

            // Re-enable AutoRunner
            runner.run()
            corunner.run()
            assertEquals(source.value * 2, target.value)
            assertEquals(source.value * 2, cotarget.value)
            source.value += 5
            runCurrent()
            assertEquals(source.value * 2, target.value)
            assertEquals(source.value * 2, cotarget.value)
            finished.complete(Unit)
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        finished.await()
        job.cancel()
        val oldValue = source.value * 2
        source.value += 5
        assertEquals(oldValue, target.value)
    }

    @Test
    fun autoRunOnce() = runTest {
        val source = MutableStateFlow(0)
        val runner = AutoRunner(SimpleCoroutineLauncher(backgroundScope), immediate = false) { get(source) }
        assertEquals(0, runner.run(once = true))
        assertFalse(runner.isActive)
        source.value = 5
        assertEquals(5, runWithResolver { get(source) })
        assertEquals(5, coRunWithResolver { get(source) })
    }
}
