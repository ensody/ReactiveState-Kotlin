package com.ensody.reactivestate.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.ensody.reactivestate.autoRun
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test

internal class LiveDataObserverTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun autoRunOnCoroutineScope() = runBlockingTest {
        val source = MutableLiveData(0)
        val target = MutableLiveData(-1)
        val job = launch {
            autoRun { target.value = 2 * get(source)!! }

            // Right after creation of the AutoRunner the values should be in sync
            assertThat(target.value).isEqualTo(0)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertThat(target.value).isEqualTo(2 * it)
            }
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        job.join()
        val oldValue = source.value!! * 2
        source.value = source.value!! + 5
        assertThat(target.value).isEqualTo(oldValue)
    }
}
