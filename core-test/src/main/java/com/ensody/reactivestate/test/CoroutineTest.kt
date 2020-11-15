package com.ensody.reactivestate.test

import com.ensody.reactivestate.dispatchers
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Rule

/**
 * Base class for unit testing coroutine based code.
 *
 * On every test run this class automatically sets [dispatchers] to the [TestCoroutineDispatcherConfig].
 *
 * Internally, this uses a [CoroutineTestRule] which does the actual [dispatchers] setup.
 * You can access the rule via [coroutineTestRule] in order to e.g. use the [TestCoroutineScope] in your
 * `@Before` setup method.
 */
public abstract class CoroutineTest {
    @get:Rule
    public val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    public open fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit): Unit =
        coroutineTestRule.runBlockingTest(block)
}
