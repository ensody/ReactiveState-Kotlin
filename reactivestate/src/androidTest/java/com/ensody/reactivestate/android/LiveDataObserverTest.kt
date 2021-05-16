package com.ensody.reactivestate.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.ensody.reactivestate.autoRun
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

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
            assertEquals(0, target.value)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                assertEquals(2 * it, target.value)
            }
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        job.join()
        val oldValue = source.value!! * 2
        source.value = source.value!! + 5
        assertEquals(oldValue, target.value)
    }
}
