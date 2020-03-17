package com.ensody.reactivestate

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DisposableTest {
    @Test
    fun disposableGroup() {
        val disposable1 = mockk<Disposable>(relaxed = true)
        val disposable2 = mockk<Disposable>(relaxed = true)
        val disposable3 = mockk<Disposable>(relaxed = true)
        val group = DisposableGroup().apply {
            add(disposable3)
            add(disposable1)
            add(disposable2)
            remove(disposable3)
        }
        group.dispose()
        verify {
            disposable1.dispose()
            disposable2.dispose()
        }
        verify(inverse = true) {
            disposable3.dispose()
        }
    }
}
