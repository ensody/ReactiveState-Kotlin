package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InterceptMutableStateFlowTest : CoroutineTest() {
    @Test
    fun interceptStateFlow() = runTest {
        val base = MutableStateFlow(0)
        val stateFlow: StateFlow<Int> = base.asStateFlow()
        val intercepted = stateFlow.toMutable { base.value = it }
        assertEquals(0, intercepted.value)
        intercepted.value = 1
        assertEquals(1, base.value)
        assertEquals(1, intercepted.value)
        // Tests compareAndSwap
        intercepted.update { 2 }
        assertEquals(2, base.value)
        assertEquals(2, intercepted.value)
    }

    @Test
    fun interceptMutableStateFlow() = runTest {
        var tracked = 0
        val intercepted = MutableStateFlow(tracked).beforeUpdate { tracked = it }
        assertEquals(0, intercepted.value)
        intercepted.value = 1
        assertEquals(1, tracked)
        assertEquals(1, intercepted.value)
    }
}
