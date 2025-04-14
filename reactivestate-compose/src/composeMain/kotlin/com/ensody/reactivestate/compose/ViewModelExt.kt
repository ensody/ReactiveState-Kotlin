package com.ensody.reactivestate.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ensody.reactivestate.ContextualErrorsFlow
import com.ensody.reactivestate.ContextualStateFlowStore
import com.ensody.reactivestate.ContextualValRoot
import com.ensody.reactivestate.CoroutineLauncher
import com.ensody.reactivestate.ExperimentalReactiveStateApi
import com.ensody.reactivestate.InMemoryStateFlowStore
import com.ensody.reactivestate.ReactiveStateContext
import com.ensody.reactivestate.triggerOnInit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

/**
 * Creates a multiplatform ViewModel. The [provider] should instantiate the object directly.
 *
 * You have to pass loading and error effect handlers, so the most basic functionality is taken care of.
 */
@ExperimentalReactiveStateApi
@Composable
public inline fun <reified VM : CoroutineLauncher> reactiveViewModel(
    key: String? = null,
    crossinline onError: (Throwable) -> Unit,
    crossinline provider: ReactiveStateContext.() -> VM,
): VM =
    onViewModel(key = key) {
        provider().also {
            it.triggerOnInit()
        }
    }.also { viewModel ->
        LaunchedEffect(viewModel) {
            ContextualErrorsFlow.get(viewModel.scope).collect { onError(it) }
        }
    }

/**
 * Creates an object living on a wrapper [ViewModel]. This allows for building more flexible (e.g. nestable) ViewModels.
 *
 * The [provider] should instantiate the object directly.
 *
 * @see [reactiveViewModel] if you want to instantiate a multiplatform ViewModel directly.
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
    return viewModel(viewModelStoreOwner = viewModelStoreOwner, key = fullKey) {
        WrapperViewModel { scope ->
            ReactiveStateContext(
                scope + ContextualValRoot() + ContextualStateFlowStore.valued { InMemoryStateFlowStore(storage) },
            ).provider()
        }
    }.value
}

/** A wrapper ViewModel used to hold an arbitrary [value]. */
public class WrapperViewModel<T : Any?>(provider: (CoroutineScope) -> T) : ViewModel() {
    public val value: T = provider(viewModelScope)
}
