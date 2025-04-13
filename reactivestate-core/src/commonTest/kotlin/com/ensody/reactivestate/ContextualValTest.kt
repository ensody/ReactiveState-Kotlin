package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class ContextualValTest : CoroutineTest() {

    @Test
    fun contextualTest() = runTest {
        assertSame(ContextualList.get(), ContextualList.get())
        assertEquals(listOf(""), ContextualList.get())
        assertEquals(0, ContextualInt.get())
        ContextualList.with(listOf("foo", "bar")) {
            assertEquals(0, ContextualInt.get())
            assertSame(ContextualList.get(), ContextualList.get())
            assertEquals(listOf("foo", "bar"), ContextualList.get())
        }

        withContext(ContextualValRoot()) {
            assertEquals(1, ContextualInt.get())
            withContext(ContextualValRoot()) {
                assertEquals(2, ContextualInt.get())
            }
            assertEquals(1, ContextualInt.get())
        }
        assertEquals(0, ContextualInt.get())
    }

    @Test
    fun contextualSuspendTest() = runTest {
        assertSame(ContextualListSuspend.get(), ContextualListSuspend.get())
        assertEquals(listOf(""), ContextualListSuspend.get())
        assertEquals(0, ContextualIntSuspend.get())
        ContextualListSuspend.with(listOf("foo", "bar")) {
            assertEquals(0, ContextualIntSuspend.get())
            assertSame(ContextualListSuspend.get(), ContextualListSuspend.get())
            assertEquals(listOf("foo", "bar"), ContextualListSuspend.get())
        }

        withContext(ContextualValRoot()) {
            assertEquals(1, ContextualIntSuspend.get())
            withContext(ContextualValRoot()) {
                assertEquals(2, ContextualIntSuspend.get())
            }
            assertEquals(1, ContextualIntSuspend.get())
        }
        assertEquals(0, ContextualIntSuspend.get())
    }
}

private var value = 0
internal val ContextualInt = ContextualVal("ContextualInt") { value++ }
internal val ContextualList = ContextualVal("ContextualList") { listOf("") }

private var valueSuspend = 0
internal val ContextualIntSuspend = ContextualValSuspend("ContextualIntSuspend") { valueSuspend++ }
internal val ContextualListSuspend = ContextualValSuspend("ContextualListSuspend") { listOf("") }
