package com.ensody.reactivestate

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

/** Returns [StateFlow.value] and tracks the observable (on the `MainScope`). */
public fun <T> Resolver.get(data: StateFlow<T>): T {
    track(data) { StateFlowObservable(data, autoRunner) }
    return data.value
}

private class StateFlowObservable<T>(
    private val data: StateFlow<T>,
    private val autoRunner: BaseAutoRunner
) : AutoRunnerObservable {
    private var observer: Job? = null

    override fun addObserver() {
        if (observer == null) {
            observer = autoRunner.launcher.launch(withLoading = null) {
                var ignore = true
                data.collect {
                    if (!ignore) {
                        autoRunner.triggerChange()
                    }
                    ignore = false
                }
            }
        }
    }

    override fun removeObserver() {
        observer?.cancel()
    }
}
