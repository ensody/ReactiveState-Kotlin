package com.ensody.reactivestate

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ObserverTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun autoRunOnCoroutineScope() = runBlockingTest {
        val source = MutableLiveDataNonNull(0)
        val target = MutableLiveDataNonNull(-1)
        val job = launch {
            val runner = autoRun { target.value = it(source) * 2 }

            // Right after creation of the AutoRunner the values should be in sync
            assertEquals(0, target.value)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertEquals(it * 2, target.value)
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
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertEquals(oldValue, target.value)
    }

    @Test
    fun derivedObservableOnCoroutineScope() = runBlockingTest {
        val source = MutableLiveDataNonNull(0)
        var target: LiveData<Int> = MutableLiveDataNonNull(-1)
        val job = launch {
            target = derived { it(source) * 2 }

            // Right after creation of the derived observable the values should be in sync
            assertEquals(0, target.value)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertEquals(it * 2, target.value)
            }
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertEquals(oldValue, target.value)
    }
}
