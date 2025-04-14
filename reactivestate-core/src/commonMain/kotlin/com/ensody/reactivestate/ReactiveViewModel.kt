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
 * For example, the [ContextualOnInit] hook can be used to launch coroutines via a [CoroutineLauncher]
 * directly from the ViewModel constructor, so you can have nicer error handling and loading indicators.
 */
@ExperimentalReactiveStateApi
public abstract class ReactiveViewModel(final override val scope: CoroutineScope) : CoroutineLauncher {
    private val emittedErrors: MutableFlow<Throwable> = ContextualErrorsFlow.get(scope)
    override val loading: MutableStateFlow<Int> = ContextualLoading.get(scope)

    override fun onError(error: Throwable) {
        emittedErrors.tryEmit(error)
    }
}

@ExperimentalReactiveStateApi
public val ContextualErrorsFlow: ContextualVal<MutableFlow<Throwable>> = ContextualVal("ContextualErrorsFlow") {
    requireContextualValRoot(it)
    MutableFlow(capacity = Channel.UNLIMITED, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
