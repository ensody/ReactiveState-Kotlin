package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
internal class SampleState(scope: CoroutineScope, store: StateFlowStore) :
    CoroutineLauncher by SimpleCoroutineLauncher(scope) {

    val counter = store.getData("counter", 0)

    fun increment() {
        counter.value += 1
    }
}

@ExperimentalCoroutinesApi
internal class StateFlowStoreTest {
    @Test
    fun conflatedQueue() = runBlockingTest {
        val store = InMemoryStateFlowStore()
        assertThat(store.contains("counter")).isFalse()

        val state = SampleState(this, store)
        assertThat(store.contains("counter")).isTrue()
        assertThat(state.counter.value).isEqualTo(0)
        assertThat(state.counter).isSameAs(store.getData("counter", -200))
        state.increment()
        assertThat(store.getData("counter", -200).value).isEqualTo(1)
    }
}
