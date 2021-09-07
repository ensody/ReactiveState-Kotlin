package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope

/**
 * Simple default implementation of a [CoroutineLauncher] which uses a given [CoroutineScope].
 *
 * Usually you'll want to use a [ReactiveState] which also does error handling.
 */
public open class SimpleCoroutineLauncher(final override val launcherScope: CoroutineScope) : CoroutineLauncher {
    final override val loading: MutableValueFlow<Int> = MutableValueFlow(0)
}
