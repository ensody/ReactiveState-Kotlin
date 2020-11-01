package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class DerivedStateFlow<T>(scope: CoroutineScope, private val observer: AutoRunCallback<T>) : StateFlow<T> {
    private var initialized = false
    private val autoRunner = AutoRunner(scope) {
        val result = observer()
        if (initialized) {
            updateValue(result)
        }
        result
    }
    private val data = MutableStateFlow(autoRunner.run())

    init {
        initialized = true
    }

    private fun updateValue(value: T) {
        data.value = value
    }

    override val replayCache: List<T> get() = data.replayCache

    override val value: T get() = data.value

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>): Unit =
        data.collect(collector)
}

public fun <T> CoroutineScope.derived(observer: AutoRunCallback<T>): StateFlow<T> =
    DerivedStateFlow(this, observer)

public fun <T> CoroutineScopeOwner.derived(observer: AutoRunCallback<T>): StateFlow<T> =
    scope.derived(observer)
