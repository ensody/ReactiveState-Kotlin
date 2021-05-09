package com.ensody.reactivestate.test

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class CoroutineTestTest : CoroutineTest() {
    var initializedSetup = false
    val mainScope = MainScope()
    var initializedMain = false

    @BeforeTest
    fun setup() {
        coroutineTestRule.testCoroutineScope.launch {
            initializedSetup = true
        }
        mainScope.launch {
            initializedMain = true
        }
    }

    @Test
    fun `test rule`() = runBlockingTest {
        advanceUntilIdle()
        assertTrue(initializedSetup)
        assertTrue(initializedMain)
    }
}
