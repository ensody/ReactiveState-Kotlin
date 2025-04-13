package com.ensody.reactivestate

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/** Returns [LiveData.getValue] and tracks the observable. */
public fun <T> Resolver.get(data: LiveData<T>): T? =
    track(data) { LiveDataObservable(data, autoRunner) }.value

private class LiveDataObservable<T>(
    private val data: LiveData<T>,
    autoRunner: BaseAutoRunner,
) : AutoRunnerObservable<T?> {
    private var ignore = false
    private val observer = Observer<T> {
        if (!ignore) {
            autoRunner.triggerChange()
        }
    }

    override val value: T? get() = data.value

    override fun addObserver() {
        // Prevent recursion and assume the value is already set correctly
        ignore = true
        data.observeForever(observer)
        ignore = false
    }

    override fun removeObserver() {
        data.removeObserver(observer)
    }
}
