package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart

public class DerivedStateFlow<T>(
    scope: CoroutineScope,
    public val lazy: Boolean = false,
    private val observer: AutoRunCallback<T>,
) : StateFlow<T> {
    private var initialized = false
    private val autoRunner = AutoRunner(scope) {
        val result = observer()
        if (initialized) {
            updateValue(result)
        }
        result
    }
    private val data = MutableStateFlow(autoRunner.run(track = !lazy))
    private val dataFlow = data.onStart {
        if (!initialized) {
            initialized = true
            data.value = autoRunner.run()
        }
    }

    init {
        initialized = !lazy
    }

    private fun updateValue(value: T) {
        data.value = value
    }

    override val replayCache: List<T> get() = data.replayCache

    override val value: T get() = data.value

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>): Unit =
        dataFlow.collect(collector)
}

public fun <T> CoroutineScope.derived(lazy: Boolean = false, observer: AutoRunCallback<T>): StateFlow<T> =
    DerivedStateFlow(scope = this, lazy = lazy, observer = observer)

public fun <T> CoroutineScopeOwner.derived(lazy: Boolean = false, observer: AutoRunCallback<T>): StateFlow<T> =
    scope.derived(lazy = lazy, observer = observer)
