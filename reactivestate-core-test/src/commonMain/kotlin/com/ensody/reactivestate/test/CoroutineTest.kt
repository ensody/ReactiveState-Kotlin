package com.ensody.reactivestate.test

import com.ensody.reactivestate.AttachedDisposables
import com.ensody.reactivestate.DisposableGroup
import com.ensody.reactivestate.derived
import com.ensody.reactivestate.dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Base class for unit testing coroutine based code.
 *
 * On every test run this class automatically sets `MainScope` and [dispatchers] to use a `TestCoroutineDispatcher`.
 *
 * Internally, this uses a [CoroutineTestRule] which does the actual [dispatchers] and `Dispatchers.setMain()` setup.
 * This allows accessing e.g. the [testScope] in your `@Before` setup method.
 *
 * Moreover, this provides an [attachedDisposables] attribute and a [collectFlow] helper, so you can activate
 * [SharingStarted.WhileSubscribed] based flows created with [derived], for example.
 */
public open class CoroutineTest(
    context: CoroutineContext = EmptyCoroutineContext,
    testDispatcherBuilder: (TestCoroutineScheduler) -> TestDispatcher = { StandardTestDispatcher(it) },
) : CoroutineTestRule(testDispatcherBuilder = testDispatcherBuilder, context = context), AttachedDisposables {

    override val attachedDisposables: DisposableGroup = DisposableGroup()

    /** Collects a flow in the background. */
    public fun <T> TestScope.collectFlow(flow: Flow<T>, collector: suspend (T) -> Unit = {}) {
        attachedDisposables.add(launch { flow.onEach(collector).collect() })
    }

    /** In addition to running the test this also disposes the [attachedDisposables] (useful with [collectFlow]). */
    public override fun runTest(block: suspend TestScope.() -> Unit): TestResult =
        super.runTest {
            try {
                block()
            } finally {
                dispose()
            }
        }
}
