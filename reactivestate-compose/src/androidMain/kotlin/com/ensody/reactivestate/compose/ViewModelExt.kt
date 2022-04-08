package com.ensody.reactivestate.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.ensody.reactivestate.ErrorEvents
import com.ensody.reactivestate.ExperimentalReactiveStateApi
import com.ensody.reactivestate.InMemoryStateFlowStore
import com.ensody.reactivestate.ReactiveState
import com.ensody.reactivestate.StateFlowStore
import com.ensody.reactivestate.compose.android.onViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a multiplatform [ReactiveState] ViewModel and observes its [ReactiveState.eventNotifier].
 *
 * The [provider] should instantiate the object directly.
 */
@ExperimentalReactiveStateApi
@Suppress("UNCHECKED_CAST")
@Composable
public inline fun <reified E : ErrorEvents, reified T : ReactiveState<E>> reactiveState(
    key: String? = null,
    crossinline provider: ReactiveStateBuildContext.() -> T,
): ReactiveStateWrapper<E, T> =
    ReactiveStateWrapper(
        (key ?: "") + ":reactiveStateEvents:${T::class.qualifiedName}",
        onViewModel(key = key, provider = provider),
    )

@ExperimentalReactiveStateApi
public class ReactiveStateWrapper<E : ErrorEvents, T : ReactiveState<E>>(
    private val key: String,
    private val reactiveState: T,
) {

    @ExperimentalReactiveStateApi
    @Composable
    public fun handlingEvents(handler: E): T {
        LaunchedEffect(key1 = key) {
            reactiveState.eventNotifier.collect { handler.it() }
        }
        return reactiveState
    }
}

/**
 * Build context for creating a [ReactiveState]/ViewModel for a UI element (e.g. a Composable or an Android Fragment).
 */
@ExperimentalReactiveStateApi
public class ReactiveStateBuildContext(
    /** A [CoroutineScope] that defines the lifecycle of the [ReactiveState]. */
    public val scope: CoroutineScope,
) {

    /** A [StateFlowStore] where you can store/load the saved instance state (similar to a `SavedStateHandle`). */
    @ExperimentalReactiveStateApi
    public val stateFlowStore: StateFlowStore
        @Composable
        get() = InMemoryStateFlowStore(rememberSaveable { mutableStateMapOf() })
}
