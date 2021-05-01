package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

internal class SuspendMutableValueFlowTest {
    @Test
    fun `value assignment behaves like MutableValueFlow`() = runBlockingTest {
        var mutations = 0
        val storage = MutableValueFlow(0)
        val data = SuspendMutableValueFlow(MutableValueFlow(0)) {
            mutations += 1
            storage.emit(it)
        }

        assertThat(data.value).isEqualTo(storage.value)

        data.set(2)
        assertThat(mutations).isEqualTo(1)
        assertThat(data.value).isEqualTo(storage.value)
        assertThat(data.value).isEqualTo(2)

        data.set(2)
        assertThat(mutations).isEqualTo(1)
        assertThat(data.value).isEqualTo(storage.value)
        assertThat(data.value).isEqualTo(2)

        data.set(2, force = true)
        assertThat(mutations).isEqualTo(2)
        assertThat(data.value).isEqualTo(storage.value)
        assertThat(data.value).isEqualTo(2)

        data.replace { it + 1 }
        assertThat(mutations).isEqualTo(3)
        assertThat(data.value).isEqualTo(storage.value)
        assertThat(data.value).isEqualTo(3)

        data.update {  }
        assertThat(mutations).isEqualTo(4)
        assertThat(data.value).isEqualTo(storage.value)
        assertThat(data.value).isEqualTo(3)
    }
}
