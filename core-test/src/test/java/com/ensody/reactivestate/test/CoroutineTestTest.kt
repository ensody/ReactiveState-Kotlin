package com.ensody.reactivestate.test

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test

internal class CoroutineTestTest : CoroutineTest() {
    val triggerSetup = CompletableDeferred<Unit>()
    var initializedSetup = false

    @Before
    fun setup() {
        coroutineTestRule.testCoroutineScope.launch {
            triggerSetup.await()
            initializedSetup = true
        }
    }

    @Test
    fun `test rule`() = runBlockingTest {
        advanceUntilIdle()
        assertThat(initializedSetup).isFalse()
        triggerSetup.complete(Unit)
        advanceUntilIdle()
        assertThat(initializedSetup).isTrue()
    }
}
