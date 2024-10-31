package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope

/**
 * Contains all values which are part of the
 */
@ExperimentalReactiveStateApi
public interface ReactiveStateContext {
    public val scope: CoroutineScope
    public val stateFlowStore: StateFlowStore
}

@ExperimentalReactiveStateApi
public fun ReactiveStateContext(scope: CoroutineScope, stateFlowStore: StateFlowStore): ReactiveStateContext =
    DefaultReactiveStateContext(scope, stateFlowStore)

private class DefaultReactiveStateContext(
    override val scope: CoroutineScope,
    override val stateFlowStore: StateFlowStore,
) : ReactiveStateContext
