package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DerivedStateFlow<T>(scope: CoroutineScope, private val observer: AutoRunCallback<T>) : StateFlow<T> {
    private var initialized = false
    private val autoRunner = AutoRunner<T>(scope) {
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

    override val value: T get() = data.value

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>) =
        data.collect(collector)
}

fun <T> CoroutineScope.derived(observer: AutoRunCallback<T>) =
    DerivedStateFlow(this, observer)

fun <T> Scoped.derived(observer: AutoRunCallback<T>) =
    scope.derived(observer)
