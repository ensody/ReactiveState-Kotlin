package com.ensody.reactivestate.test

import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Rule

/**
 * Helper interface for unit testing coroutine based code. See [CoroutineTest] for details.
 */
public interface CoroutineTestRuleOwner {
    @get:Rule
    public val coroutineTestRule: CoroutineTestRule

    public fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit): Unit =
        coroutineTestRule.runBlockingTest(block)
}
