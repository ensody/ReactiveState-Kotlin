package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.CoroutineScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class SampleState(scope: CoroutineScope, store: StateFlowStore) :
    CoroutineLauncher by SimpleCoroutineLauncher(scope) {

    val counter by store.getData(0)

    fun increment() {
        counter.value += 1
    }
}

internal class StateFlowStoreTest : CoroutineTest() {
    @Test
    fun conflatedQueue() = runTest {
        val store = InMemoryStateFlowStore()
        assertFalse(store.contains("counter"))

        val state = SampleState(this, store)
        assertFalse(store.contains("counter"))
        state.counter
        assertTrue(store.contains("counter"))
        assertEquals(0, state.counter.value)
        assertSame(store.getData("counter", -200), state.counter)
        state.increment()
        assertEquals(1, store.getData("counter", -200).value)
    }
}
