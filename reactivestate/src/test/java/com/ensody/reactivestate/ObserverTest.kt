package com.ensody.reactivestate

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ObserverTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun nonNull() {
        val source = MutableLiveData(0)
        val target = (source as LiveData<Int>).fixValueType()

        // Even without observers the value should stay in sync
        assertThat(target.value).isEqualTo(0)
        source.value = 5
        assertThat(target.value).isEqualTo(5)

        // With observers we should also get notified
        var notifications = 0
        target.observeForever {
            notifications += 1
        }
        assertThat(notifications).isEqualTo(1)
        assertThat(target.value).isEqualTo(5)
        source.value = 10
        assertThat(target.value).isEqualTo(10)
        assertThat(notifications).isEqualTo(2)
    }

    @Test
    fun autoRunOnCoroutineScope() = runBlockingTest {
        val source = MutableLiveDataNonNull(0)
        val target = MutableLiveData(-1).fixValueType()
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
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertThat(target.value).isEqualTo(oldValue)
    }

    @Test
    fun derivedObservableOnCoroutineScope() = runBlockingTest {
        val source = MutableLiveDataNonNull(0)
        var target: LiveData<Int> = MutableLiveDataNonNull(-1)
        val job = launch {
            target = derived { 2 * get(source) }

            // Right after creation of the derived observable the values should be in sync
            assertThat(target.value).isEqualTo(0)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertThat(target.value).isEqualTo(2 * it)
            }
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        job.join()
        val oldValue = source.value * 2
        source.value += 5
        assertThat(target.value).isEqualTo(oldValue)
    }
}
