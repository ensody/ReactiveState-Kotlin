package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Simple default implementation of a [CoroutineLauncher] which uses a given [CoroutineScope].
 *
 * Usually you'll want to use a [ReactiveViewModel] or maybe [BaseReactiveState].
 */
public open class SimpleCoroutineLauncher(final override val scope: CoroutineScope) : CoroutineLauncher {
    final override val loading: MutableStateFlow<Int> =
        runCatching { ContextualLoading.get(scope) }.getOrElse { MutableStateFlow(0) }
}
