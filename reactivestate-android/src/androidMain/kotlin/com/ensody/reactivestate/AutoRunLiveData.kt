package com.ensody.reactivestate

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/** Returns [LiveData.getValue] and tracks the observable. */
public fun <T> Resolver.get(data: LiveData<T>): T? =
    track(data) { LiveDataObservable(data) }.value

private class LiveDataObservable<T>(
    private val data: LiveData<T>,
) : AutoRunnerObservable<T?> {
    private lateinit var autoRunner: BaseAutoRunner
    private var ignore = false
    private val observer = Observer<T> {
        if (!ignore) {
            autoRunner.triggerChange()
        }
    }

    override val value: T? get() = data.value

    override fun addObserver(autoRunner: BaseAutoRunner) {
        // Prevent recursion and assume the value is already set correctly
        ignore = true
        this.autoRunner = autoRunner
        data.observeForever(observer)
        ignore = false
    }

    override fun removeObserver(autoRunner: BaseAutoRunner) {
        data.removeObserver(observer)
    }
}
