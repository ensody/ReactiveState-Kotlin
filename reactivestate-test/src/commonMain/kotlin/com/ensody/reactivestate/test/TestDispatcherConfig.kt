package com.ensody.reactivestate.test

import com.ensody.reactivestate.CoroutineDispatcherConfig
import kotlinx.coroutines.test.TestDispatcher

/** A [CoroutineDispatcherConfig] for unit tests - dispatching everything to the given [dispatcher]. */
public class TestDispatcherConfig(public val dispatcher: TestDispatcher) : CoroutineDispatcherConfig {
    override val default: TestDispatcher = dispatcher
    override val main: TestDispatcher = dispatcher
    override val io: TestDispatcher = dispatcher
    override val unconfined: TestDispatcher = dispatcher
}
