package com.ensody.reactivestate

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
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
            val runner = autoRun { target.value = 2 * it(source) }

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
            target = derived { 2 * it(source) }

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
