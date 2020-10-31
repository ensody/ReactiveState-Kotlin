package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class ObserverTest {
    @Test
    fun autoRunOnCoroutineScope() = runBlockingTest {
        val source = MutableValueFlow(0)
        val target = MutableValueFlow(-1)
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
            cancel()
        }
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertThat(target.value).isEqualTo(oldValue)
    }

    @Test
    fun derivedObservableOnCoroutineScope() = runBlockingTest {
        val source = MutableValueFlow(0)
        var target: StateFlow<Int> = MutableValueFlow(-1)
        val job = launch {
            target = derived { 2 * get(source) }

            // Right after creation of the derived observable the values should be in sync
            assertThat(target.value).isEqualTo(0)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertThat(target.value).isEqualTo(2 * it)
            }
            cancel()
        }
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertThat(target.value).isEqualTo(oldValue)
    }
}
