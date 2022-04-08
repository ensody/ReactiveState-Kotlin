package com.ensody.reactivestate.test

import com.ensody.reactivestate.DefaultCoroutineDispatcherConfig
import com.ensody.reactivestate.dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Helper class for unit Tests that sets up [dispatchers] with a [TestDispatcherConfig] on every test run.
 *
 * Also, [Dispatchers.Main] is set to a [TestScope].
 *
 * This rule allows for e.g. setup methods with `@Before` which need access to the [TestScope].
 *
 * Use the [runTest] method provided by this rule instead of the one provided by the coroutines library.
 *
 * You can also access the [testScope] and [testDispatcher] in case you need them to e.g. launch
 * some background process during each test.
 */
public open class CoroutineTestRule(
    testDispatcherBuilder: (TestCoroutineScheduler) -> TestDispatcher = { StandardTestDispatcher(it) },
) {
    public val testScope: TestScope = TestScope()
    public val testDispatcher: TestDispatcher = testDispatcherBuilder(testScope.testScheduler)

    @Deprecated("Use testScope", ReplaceWith("testScope"))
    public val testCoroutineScope: TestScope get() = testScope
    @Deprecated("Use testDispatcher", ReplaceWith("testDispatcher"))
    public val testCoroutineDispatcher: TestDispatcher get() = testDispatcher

    init {
        enterCoroutineTest()
    }

    public val mainScope: CoroutineScope = MainScope()

    public open fun runTest(block: suspend TestScope.() -> Unit): TestResult =
        testScope.runTest {
            try {
                block()
            } finally {
                mainScope.cancel("Exiting test")
            }
        }

    @Deprecated("Use runTest instead", ReplaceWith("runTest(block)"))
    public open fun runBlockingTest(block: suspend TestScope.() -> Unit): TestResult =
        runTest(block)

    public fun enterCoroutineTest() {
        dispatchers = TestDispatcherConfig(testDispatcher)
        Dispatchers.setMain(testDispatcher)
    }

    public fun exitCoroutineTest() {
        Dispatchers.resetMain()
        dispatchers = DefaultCoroutineDispatcherConfig
    }
}
