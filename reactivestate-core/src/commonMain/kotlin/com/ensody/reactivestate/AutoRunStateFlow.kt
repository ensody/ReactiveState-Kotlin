package com.ensody.reactivestate

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/** Returns [StateFlow.value] and tracks the observable (on the `MainScope`). */
public fun <T> Resolver.get(data: StateFlow<T>): T =
    track(data) { StateFlowObservable(data, autoRunner) }.value

private class StateFlowObservable<T>(
    private val data: StateFlow<T>,
    private val autoRunner: BaseAutoRunner,
) : AutoRunnerObservable<T> {
    private var observer: Job? = null

    override val value: T get() = data.value

    override fun addObserver() {
        if (observer == null) {
            var ignore: Wrapped<T>? = Wrapped(data.value)
            observer = autoRunner.launcher.launch(withLoading = null) {
                data.collect { value ->
                    if (ignore?.let { it.value != value } != false) {
                        autoRunner.triggerChange()
                    }
                    ignore = null
                }
            }
        }
    }

    override fun removeObserver() {
        observer?.cancel()
        observer = null
    }
}
