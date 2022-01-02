package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserverTest : CoroutineTest() {
    @Test
    fun autoRunOnCoroutineScope() = runBlockingTest {
        val source = MutableValueFlow(0)
        val target = MutableValueFlow(-1)
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
            cancel()
        }
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertEquals(oldValue, target.value)
    }

    @Test
    fun derivedObservableOnCoroutineScope() = runBlockingTest {
        val source = MutableValueFlow(0)
        lateinit var target: StateFlow<Int>
        lateinit var scopelessTarget: StateFlow<Int>
        val job = launch {
            target = derived { 2 * get(source) }
            scopelessTarget = scopelessDerived { 2 * get(source) }
            val lazyTarget = derived(0, Lazily) { 2 * get(source) }
            val superLazyTarget = derived(0, WhileSubscribed()) { 2 * get(source) }
            val superLazyScopelessTarget = derivedWhileSubscribed(0) { 2 * get(source) }
            val asyncTarget = derived(-1, Eagerly) { delay(100); 2 * get(source) }

            // Right after creation of the derived observable the values should be in sync
            assertEquals(0, target.value)
            assertEquals(0, scopelessTarget.value)
            assertEquals(0, lazyTarget.value)
            assertEquals(0, superLazyTarget.value)
            assertEquals(0, superLazyScopelessTarget.value)
            assertEquals(-1, asyncTarget.value)

            advanceTimeBy(120)
            assertEquals(0, asyncTarget.value)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertEquals(2 * it, target.value)
                assertEquals(2 * it, scopelessTarget.value)
                assertEquals(0, lazyTarget.value)
                assertEquals(0, superLazyTarget.value)
                assertEquals(0, superLazyScopelessTarget.value)
            }

            // We react with a delay
            assertEquals(0, asyncTarget.value)
            advanceTimeBy(120)
            assertEquals(2 * 10, asyncTarget.value)

            val lazyJob = launch { lazyTarget.collect() }
            val superLazyJob = launch { superLazyTarget.collect() }
            val superLazyScopelessJob = launch { superLazyScopelessTarget.collect() }
            advanceUntilIdle()

            // Once somebody collects the lazy derived flow, the value gets updated continuously
            assertEquals(2 * 10, lazyTarget.value)
            listOf(2, 5, 10).forEach {
                source.value = it
                assertEquals(2 * it, lazyTarget.value)
                assertEquals(2 * it, superLazyTarget.value)
                assertEquals(2 * it, superLazyScopelessTarget.value)
            }

            lazyJob.cancel()
            superLazyJob.cancel()
            superLazyScopelessJob.cancel()

            assertEquals(2 * 10, lazyTarget.value)
            assertEquals(2 * 10, superLazyTarget.value)
            assertEquals(2 * 10, superLazyScopelessTarget.value)
            listOf(2, 5, 10).forEach {
                source.value = it
                // Even when nobody is listening anymore, we continue updating
                assertEquals(2 * it, lazyTarget.value)
                // We stop updating when nobody listens
                assertEquals(2 * 10, superLazyTarget.value)
                assertEquals(2 * 10, superLazyScopelessTarget.value)
            }

            cancel()
        }
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertEquals(oldValue, target.value)
        assertEquals(oldValue + 10, scopelessTarget.value)
    }

    private fun <T> scopelessDerived(observer: AutoRunCallback<T>) = derived(observer)
}
