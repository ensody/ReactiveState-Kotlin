package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class UtilsTest : CoroutineTest() {
    @Test
    fun testIfTake() = runTest {
        val result = ifTake(true) {
            delay(100)
            5
        }
        assertEquals(5, result)
        assertNull(ifTake(false) { 2 })
    }

    @Test
    fun testUnlessTake() = runTest {
        val result = unlessTake(false) {
            delay(100)
            5
        }
        assertEquals(5, result)
        assertNull(unlessTake(true) { 2 })
    }

    @Test
    fun testApplyIf() = runTest {
        var done = false
        val result = applyIf(true) {
            delay(100)
            done = true
        }
        assertSame(this, result)
        assertTrue(done)

        done = false
        applyIf(false) { done = true }
        assertFalse(done)
    }

    @Test
    fun testRunIf() = runTest {
        val result = runIf(true) {
            assertSame(this@runTest, this)
            delay(100)
            5
        }
        assertEquals(5, result)
        assertNull(runIf(false) { 2 })
    }
}
