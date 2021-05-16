package com.ensody.reactivestate

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DisposableTest {
    @Test
    fun disposableGroup() = runBlockingTest {
        val disposable1 = mockk<Disposable>(relaxed = true)
        val disposable2 = mockk<Disposable>(relaxed = true)
        val disposable3 = mockk<Disposable>(relaxed = true)
        val job = launch { CompletableDeferred<Unit>().await() }
        val group = DisposableGroup().apply {
            add(disposable3)
            add(disposable1)
            add(disposable2)
            assertEquals(3, size)
            remove(disposable3)
            assertEquals(2, size)
            add(job)
            assertEquals(3, size)
            remove(job)
            assertEquals(2, size)
            add(job)
            assertEquals(3, size)
        }
        assertTrue(job.isActive)
        group.dispose()
        assertFalse(job.isActive)
        assertTrue(job.isCancelled)
        verify {
            disposable1.dispose()
            disposable2.dispose()
        }
        verify(inverse = true) {
            disposable3.dispose()
        }
    }
}
