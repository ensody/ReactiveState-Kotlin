package com.ensody.reactivestate.test

import com.ensody.reactivestate.CoroutineDispatcherConfig
import kotlinx.coroutines.test.TestCoroutineDispatcher

/** A [CoroutineDispatcherConfig] for unit tests - dispatching everything to the given [dispatcher]. */
public class TestCoroutineDispatcherConfig(public val dispatcher: TestCoroutineDispatcher) : CoroutineDispatcherConfig {
    override val default: TestCoroutineDispatcher = dispatcher
    override val main: TestCoroutineDispatcher = dispatcher
    override val io: TestCoroutineDispatcher = dispatcher
    override val unconfined: TestCoroutineDispatcher = dispatcher
}
