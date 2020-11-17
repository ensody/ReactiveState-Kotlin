package com.ensody.reactivestate.test

import assertk.assertThat
import assertk.assertions.isTrue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test

internal class CoroutineTestTest : CoroutineTestRuleOwner by CoroutineTest() {
    var initializedSetup = false
    val mainScope = MainScope()
    var initializedMain = false

    @Before
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
        assertThat(initializedSetup).isTrue()
        assertThat(initializedMain).isTrue()
    }
}
