package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ReducingStateFlowTest : CoroutineTest() {
    @Test
    fun `ReducingStateFlow recomputes dynamically`() = runBlockingTest {
        launch {
            val sumStateFlow = ReducingStateFlow<Int, Int>(this) { it.sum() }
            val boolStateFlow = ReducingStateFlow<Int, Boolean>(this) { it.sum() > 0 }
            assertFalse(boolStateFlow.value)
            assertEquals(0, sumStateFlow.value)

            val flow1 = MutableStateFlow(1)
            val flow2 = MutableStateFlow(0)
            for (reducingStateFlow in listOf(boolStateFlow, sumStateFlow)) {
                for (flow in listOf(flow1, flow2)) {
                    reducingStateFlow.add(flow)
                }
            }
            assertTrue(boolStateFlow.value)
            assertEquals(1, sumStateFlow.value)

            flow1.value -= 1
            assertFalse(boolStateFlow.value)
            assertEquals(0, sumStateFlow.value)

            cancel()
        }.join()
    }
}
