package com.ensody.reactivestate.test

import com.ensody.reactivestate.DefaultCoroutineDispatcherConfig
import com.ensody.reactivestate.dispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * This test rule sets up [dispatchers] with a [TestCoroutineDispatcherConfig] on every test run.
 *
 * Also, [Dispatchers.Main] is set to a [TestCoroutineScope].
 *
 * This rule allows for e.g. setup methods with `@Before` which need access to the [TestCoroutineScope].
 *
 * Use the [runBlockingTest] method provided by this rule instead of the one provided by the coroutines library.
 *
 * You can also access the [testCoroutineScope] and [testCoroutineDispatcher] in case you need them to e.g. launch
 * some background process during each test.
 */
public class CoroutineTestRule : TestWatcher() {
    public val testCoroutineDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
    public val testCoroutineScope: TestCoroutineScope = TestCoroutineScope(testCoroutineDispatcher)

    public fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit): Unit =
        testCoroutineScope.runBlockingTest(block)

    override fun starting(description: Description?) {
        super.starting(description)
        dispatchers = TestCoroutineDispatcherConfig(testCoroutineDispatcher)
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
        dispatchers = DefaultCoroutineDispatcherConfig
        testCoroutineDispatcher.cleanupTestCoroutines()
    }
}
