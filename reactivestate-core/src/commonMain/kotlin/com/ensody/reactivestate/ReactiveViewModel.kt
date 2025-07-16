package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Base class for ViewModels.
 *
 * The [scope] is meant to be filled with [ContextualVal] entries.
 *
 * The [onInit] hook can be used to launch coroutines via a [CoroutineLauncher] directly from the ViewModel constructor,
 * so you can have nicer error handling and loading indicators.
 */
@ExperimentalReactiveStateApi
public abstract class ReactiveViewModel(public final override val scope: CoroutineScope) : CoroutineLauncher {
    public val onInit: OnInit = ContextualStore.get(scope).getOrPut(OnInitKey) { OnInit(this) }
    private val emittedErrors: MutableFlow<Throwable> = ContextualErrorsFlow.get(scope)
    override val loading: MutableStateFlow<Int> = ContextualLoading.get(scope)
    public val stateFlowStore: StateFlowStore by lazy { ContextualStateFlowStore.get(scope) }

    override fun onError(error: Throwable) {
        emittedErrors.tryEmit(error)
    }
}

private val OnInitKey = ContextualValStore.Key<OnInit>()

@ExperimentalReactiveStateApi
public val ContextualErrorsFlow: ContextualVal<MutableFlow<Throwable>> = ContextualVal("ContextualErrorsFlow") {
    requireContextualValRoot(it)
    MutableFlow(capacity = Channel.UNLIMITED, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
