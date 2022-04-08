package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SuspendMutableValueFlowTest : CoroutineTest() {
    @Test
    fun valueAssignmentBehavesLikeMutableValueFlow() = runTest {
        var mutations = 0
        val storage = MutableValueFlow(Counter(0))
        val data = SuspendMutableValueFlow(Counter(0)) {
            mutations += 1
            storage.emit(it)
        }

        assertEquals(storage.value, data.value)

        data.set(Counter(2))
        assertEquals(1, mutations)
        assertEquals(storage.value, data.value)
        assertEquals(2, data.value.count)

        data.set(Counter(2))
        assertEquals(1, mutations)
        assertEquals(storage.value, data.value)
        assertEquals(2, data.value.count)

        data.set(Counter(2), force = true)
        assertEquals(2, mutations)
        assertEquals(storage.value, data.value)
        assertEquals(2, data.value.count)

        data.replace { copy(count = count + 1) }
        assertEquals(3, mutations)
        assertEquals(storage.value, data.value)
        assertEquals(3, data.value.count)

        data.update { }
        assertEquals(4, mutations)
        assertEquals(storage.value, data.value)
        assertEquals(3, data.value.count)

        data.update { it.count += 1 }
        assertEquals(5, mutations)
        assertEquals(storage.value, data.value)
        assertEquals(4, data.value.count)

        data.updateThis { count += 1 }
        assertEquals(6, mutations)
        assertEquals(storage.value, data.value)
        assertEquals(5, data.value.count)
    }
}
