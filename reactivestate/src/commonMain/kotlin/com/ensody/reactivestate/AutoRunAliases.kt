package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.transform

/** Observer callback used by [autoRun] and [AutoRunner]. */
public typealias AutoRunCallback<T> = Resolver.() -> T

/** onChange callback used by [autoRun] and [AutoRunner]. */
public typealias AutoRunOnChangeCallback<T> = (AutoRunner<T>) -> Unit

/** Observer callback used by suspendable [coAutoRun] and [CoAutoRunner]. */
public typealias CoAutoRunCallback<T> = suspend Resolver.() -> T

/** onChange callback used by suspendable [coAutoRun] and [CoAutoRunner]. */
public typealias CoAutoRunOnChangeCallback<T> = suspend (CoAutoRunner<T>) -> Unit

/** Collector of the change events used by [coAutoRun] and [CoAutoRunner]. */
public typealias AutoRunFlowTransformer = DerivedFlowTransformer<Unit>

/** Collector of the change events used by [derived]. */
public typealias DerivedFlowTransformer<T> = ApplyFlowTransform<Unit, T>

/** A function applying a [Flow.transform] and possibly additional [Flow] configurations. */
public typealias ApplyFlowTransform<T, R> = Flow<T>.(FlowTransform<T, R>) -> Flow<R>

/** A function which can be passed to [Flow.transform]. */
public typealias FlowTransform<T, R> = suspend FlowCollector<R>.(T) -> Unit

/** A factory function creating a StateFlowStore. */
public typealias StateFlowStoreFactory = (CoroutineScope) -> StateFlowStore
