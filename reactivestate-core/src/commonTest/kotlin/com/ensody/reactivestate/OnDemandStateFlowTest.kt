package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OnDemandStateFlowTest : CoroutineTest() {

    @Test
    fun onDemandTest() = runTest {
        val source = MutableStateFlow(0)
        val flow = OnDemandStateFlow<Int>({ source.value }) {
            source.collectLatest { value = it }
        }
        assertEquals(0, flow.value)
        source.value = 1
        assertEquals(1, flow.value)

        var data = -1
        source.value = 2
        backgroundScope.launch { flow.collectLatest { data = it } }
        runCurrent()
        assertEquals(2, data)
        source.value = 3
        runCurrent()
        assertEquals(3, data)
    }
}
