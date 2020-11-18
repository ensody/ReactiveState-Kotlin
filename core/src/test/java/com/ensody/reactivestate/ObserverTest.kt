package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
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
        lateinit var target: StateFlow<Int>
        val job = launch {
            target = derived(Eagerly) { 2 * get(source) }
            val lazyTarget = derived(Lazily) { 2 * get(source) }
            val superLazyTarget = derived(WhileSubscribed()) { 2 * get(source) }

            // Right after creation of the derived observable the values should be in sync
            assertThat(target.value).isEqualTo(0)
            assertThat(lazyTarget.value).isEqualTo(0)
            assertThat(superLazyTarget.value).isEqualTo(0)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertThat(target.value).isEqualTo(2 * it)
                assertThat(lazyTarget.value).isEqualTo(0)
                assertThat(superLazyTarget.value).isEqualTo(0)
            }

            val lazyJob = launch { lazyTarget.collect() }
            val superLazyJob = launch { superLazyTarget.collect() }
            advanceUntilIdle()

            // Once somebody collects the lazy derived flow, the value gets updated continuously
            assertThat(lazyTarget.value).isEqualTo(2 * 10)
            listOf(2, 5, 10).forEach {
                source.value = it
                assertThat(lazyTarget.value).isEqualTo(2 * it)
                assertThat(superLazyTarget.value).isEqualTo(2 * it)
            }

            lazyJob.cancel()
            superLazyJob.cancel()

            assertThat(lazyTarget.value).isEqualTo(2 * 10)
            assertThat(superLazyTarget.value).isEqualTo(2 * 10)
            listOf(2, 5, 10).forEach {
                source.value = it
                // Even when nobody is listening anymore, we continue updating
                assertThat(lazyTarget.value).isEqualTo(2 * it)
                // We stop updating when nobody listens
                assertThat(superLazyTarget.value).isEqualTo(2 * 10)
            }

            cancel()
        }
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertThat(target.value).isEqualTo(oldValue)
    }
}
