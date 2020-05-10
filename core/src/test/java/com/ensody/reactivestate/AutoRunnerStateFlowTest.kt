package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class AutoRunnerStateFlowTest {
    @Test
    fun `autoRun with StateFlow`() = runBlockingTest {
        val source = MutableStateFlow(0)
        val target = MutableStateFlow(-1)
        val finished = CompletableDeferred<Unit>()
        val job = launch {
            val runner = autoRun { target.value = 2 * get(source) }

            // Right after creation of the AutoRunner the values should be in sync
            assertThat(target.value).isEqualTo(0)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertThat(target.value).isEqualTo(2 * it)
            }

            // Test disposing (target.value is out of sync)
            runner.dispose()
            val oldValue = target.value
            source.value += 5
            assertThat(target.value).isEqualTo(oldValue)

            // Re-enable AutoRunner
            runner.run()
            assertThat(target.value).isEqualTo(source.value * 2)
            source.value += 5
            assertThat(target.value).isEqualTo(source.value * 2)
            finished.complete(Unit)
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        finished.await()
        job.cancel()
        val oldValue = source.value * 2
        source.value += 5
        assertThat(target.value).isEqualTo(oldValue)
    }
}
