package com.ensody.reactivestate.android

import androidx.lifecycle.MutableLiveData
import com.ensody.reactivestate.autoRun
import com.ensody.reactivestate.get
import com.ensody.reactivestate.test.AndroidCoroutineTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

internal class LiveDataObserverTest : AndroidCoroutineTest() {
    @Test
    fun autoRunOnCoroutineScope() = runTest {
        val source = MutableLiveData(0)
        val target = MutableLiveData(-1)
        val job = launch {
            val runner = autoRun { target.value = 2 * get(source)!! }

            // Right after creation of the AutoRunner the values should be in sync
            assertEquals(0, target.value)

            // Setting value multiple times should work
            listOf(2, 5, 10).forEach {
                source.value = it
                runCurrent()
                assertEquals(2 * it, target.value)
            }

            runner.dispose()
        }
        // The underlying AutoRunner should be automatically disposed because its scope has ended
        job.join()
        val oldValue = source.value!! * 2
        source.value = source.value!! + 5
        runCurrent()
        assertEquals(oldValue, target.value)
    }
}
