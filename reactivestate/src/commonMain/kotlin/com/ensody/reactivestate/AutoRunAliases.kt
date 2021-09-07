package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/** Observer callback used by [autoRun] and [AutoRunner]. */
public typealias AutoRunCallback<T> = Resolver.() -> T

/** onChange callback used by [autoRun] and [AutoRunner]. */
public typealias AutoRunOnChangeCallback<T> = (AutoRunner<T>) -> Unit

/** Observer callback used by suspendable [coAutoRun] and [CoAutoRunner]. */
public typealias CoAutoRunCallback<T> = suspend Resolver.() -> T

/** onChange callback used by suspendable [coAutoRun] and [CoAutoRunner]. */
public typealias CoAutoRunOnChangeCallback<T> = suspend (CoAutoRunner<T>) -> Unit

/** Collector of the change events used by [coAutoRun] and [CoAutoRunner]. */
public typealias AutoRunFlowTransformer = Flow<suspend () -> Unit>.() -> Flow<Unit>

/** A factory function creating a StateFlowStore. */
public typealias StateFlowStoreFactory = (CoroutineScope) -> StateFlowStore
