package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Test

internal class ReducingStateFlowTest : CoroutineTest() {
    @Test
    fun `ReducingStateFlow recomputes dynamically`() = runBlockingTest {
        launch {
            val sumStateFlow = ReducingStateFlow<Int, Int>(this) { it.sum() }
            val boolStateFlow = ReducingStateFlow<Int, Boolean>(this) { it.sum() > 0 }
            assertThat(boolStateFlow.value).isFalse()
            assertThat(sumStateFlow.value).isEqualTo(0)

            val flow1 = MutableStateFlow(1)
            val flow2 = MutableStateFlow(0)
            for (reducingStateFlow in listOf(boolStateFlow, sumStateFlow)) {
                for (flow in listOf(flow1, flow2)) {
                    reducingStateFlow.add(flow)
                }
            }
            assertThat(boolStateFlow.value).isTrue()
            assertThat(sumStateFlow.value).isEqualTo(1)

            flow1.value -= 1
            assertThat(boolStateFlow.value).isFalse()
            assertThat(sumStateFlow.value).isEqualTo(0)

            cancel()
        }.join()
    }
}
