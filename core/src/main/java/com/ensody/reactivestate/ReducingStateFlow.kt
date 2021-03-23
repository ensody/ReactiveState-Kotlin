package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** A [StateFlow] that computes its output based on a dynamic list of other [StateFlow]s. */
public class ReducingStateFlow<I, O> private constructor(
    private val flows: MutableValueFlow<MutableList<StateFlow<I>>>,
    flow: StateFlow<O>,
) : StateFlow<O> by flow {
    /** Constructs a [ReducingStateFlow] with the given [scope] and [reducer] function. */
    public constructor(
        scope: CoroutineScope,
        reducer: (List<I>) -> O,
    ) : this(scope, reducer, MutableValueFlow(mutableListOf()))

    private constructor(
        scope: CoroutineScope,
        reducer: (List<I>) -> O,
        states: MutableValueFlow<MutableList<StateFlow<I>>>,
    ) : this(states, scope.derived { reducer(get(states).map { get(it) }) })

    public fun add(flow: StateFlow<I>) {
        flows.update { it.add(flow) }
    }

    public fun remove(flow: StateFlow<I>) {
        flows.update { it.remove(flow) }
    }
}
