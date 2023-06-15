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
import com.ensody.reactivestate.handleEvents
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a multiplatform [ReactiveState] ViewModel and observes its [ReactiveState.eventNotifier].
 *
 * The [provider] should instantiate the object directly.
 */
@ExperimentalReactiveStateApi
@Suppress("UNCHECKED_CAST")
@Composable
public inline fun <reified E : ErrorEvents, reified VM : ReactiveState<E>> E.reactiveState(
    key: String? = null,
    crossinline observeLoadingEffect: @Composable (viewModel: VM) -> Unit,
    crossinline provider: ReactiveStateBuildContext.() -> VM,
): VM {
    val viewModel = onViewModel(key = key, provider = provider)
    observeLoadingEffect(viewModel)
    LaunchedEffect(this, viewModel.eventNotifier) {
        viewModel.eventNotifier.handleEvents(this@reactiveState)
    }
    return viewModel
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
