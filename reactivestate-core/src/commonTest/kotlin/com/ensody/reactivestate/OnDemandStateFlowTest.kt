package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OnDemandStateFlowTest : CoroutineTest() {

    @Test
    fun onDemandTest() = runTest {
        val source = MutableStateFlow(0)
        val flow = callbackFlow {
            source.collectLatest { send(it) }
        }.stateOnDemand {
            source.value
        }
        assertEquals(0, flow.value)
        source.value = 1
        assertEquals(1, flow.value)

        var data = -1
        source.value = 2
        backgroundScope.launch { flow.collect { data = it } }
        runCurrent()
        assertEquals(2, flow.value)
        assertEquals(2, data)
        source.value = 3
        assertEquals(3, flow.value)
        runCurrent()
        assertEquals(3, data)
    }

    @Test
    fun directCollect() = runTest {
        val source = MutableStateFlow(0)
        callbackFlow {
            source.collectLatest { send(it) }
        }.stateOnDemand {
            source.value
        }.first()
    }
}
