package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope

/**
 * An object whose lifetime is bound to a given [CoroutineScope].
 *
 * This is useful in UI-based code where you want to keep state and business logic separate from the
 * UI and the UI framework.
 *
 * Since the lifetime is bound to [scope] you can depend on automatic cleanups e.g. when using
 * [autoRun] or [workQueue].
 */
abstract class Scoped(val scope: CoroutineScope) : Disposable {
    private val disposable = disposeOnCompletionOf(scope)

    override fun dispose() {
        disposable.dispose()
    }
}
