package com.ensody.reactivestate.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ensody.reactivestate.ErrorEvents
import com.ensody.reactivestate.ExperimentalReactiveStateApi
import com.ensody.reactivestate.InMemoryStateFlowStore
import com.ensody.reactivestate.OnReactiveStateAttached
import com.ensody.reactivestate.OnReactiveStateAttachedTo
import com.ensody.reactivestate.ReactiveState
import com.ensody.reactivestate.ReactiveStateContext
import com.ensody.reactivestate.ReactiveViewModel
import com.ensody.reactivestate.ReactiveViewModelContext
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
public inline fun <reified VM : ReactiveViewModel> ErrorEvents.reactiveViewModel(
    key: String? = null,
    crossinline observeLoadingEffect: @Composable (viewModel: VM) -> Unit,
    crossinline provider: ReactiveViewModelContext.() -> VM,
): VM =
    reactiveState(key = key, observeLoadingEffect = observeLoadingEffect) {
        ReactiveViewModelContext(this).provider()
    }

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
    crossinline provider: ReactiveStateContext.() -> VM,
): VM {
    val viewModel = onViewModel(key = key, provider = provider)
    observeLoadingEffect(viewModel)
    LaunchedEffect(this, viewModel.eventNotifier) {
        viewModel.eventNotifier.handleEvents(this@reactiveState)
        (this@reactiveState as? OnReactiveStateAttached)?.onReactiveStateAttached(viewModel)
        (viewModel as? OnReactiveStateAttachedTo)?.onReactiveStateAttachedTo(this@reactiveState)
    }
    return viewModel
}

/**
 * Creates an object living on a wrapper [ViewModel]. This allows for building more flexible (e.g. nestable) ViewModels.
 *
 * The [provider] should instantiate the object directly.
 *
 * @see [reactiveState] if you want to instantiate a multiplatform [ReactiveState] ViewModel directly.
 */
@ExperimentalReactiveStateApi
@Composable
public inline fun <reified T : Any?> onViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    crossinline provider: ReactiveStateContext.() -> T,
): T {
    // TODO: Use qualifiedName once JS supports it
    val fullKey = (key ?: "") + ":onViewModel:${T::class.simpleName}"
    val storage = rememberSaveable<SnapshotStateMap<String, Any?>> { mutableStateMapOf() }
    val stateFlowStore = remember { InMemoryStateFlowStore(storage) }
    return viewModel(viewModelStoreOwner = viewModelStoreOwner, key = fullKey) {
        WrapperViewModel { scope -> ReactiveStateContext(scope, stateFlowStore).provider() }
    }.value
}

/** A wrapper ViewModel used to hold an arbitrary [value]. */
public class WrapperViewModel<T : Any?>(provider: (CoroutineScope) -> T) : ViewModel() {
    public val value: T = provider(viewModelScope)
}
