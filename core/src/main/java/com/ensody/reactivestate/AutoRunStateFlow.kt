package com.ensody.reactivestate

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

/** Returns [StateFlow.value] and tracks the observable (on the `MainScope`). */
public fun <T> Resolver.get(data: StateFlow<T>): T = track(data).value

private fun <T, D : StateFlow<T>> Resolver.track(data: D): D =
    track(data) { StateFlowObservable(data, autoRunner) }

private class StateFlowObservable(
    private val data: StateFlow<*>,
    private val autoRunner: BaseAutoRunner
) : AutoRunnerObservable {
    private var observer: Job? = null

    override fun addObserver() {
        if (observer == null) {
            observer = autoRunner.launcher.launch(withLoading = false) {
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
