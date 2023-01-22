package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DisposableTest : CoroutineTest() {
    @Test
    fun disposableGroup() = runTest {
        val disposable1 = MockDisposable()
        val disposable2 = MockDisposable()
        val disposable3 = MockDisposable()
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
        assertEquals(1, disposable1.calls)
        assertEquals(1, disposable2.calls)
        assertEquals(0, disposable3.calls)
    }
}

internal class MockDisposable : Disposable {
    var calls: Int = 0

    override fun dispose() {
        calls += 1
    }
}
