package com.ensody.reactivestate.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ensody.reactivestate.ContextualErrorsFlow
import com.ensody.reactivestate.ContextualStateFlowStore
import com.ensody.reactivestate.ContextualValRoot
import com.ensody.reactivestate.DI
import com.ensody.reactivestate.ExperimentalReactiveStateApi
import com.ensody.reactivestate.InMemoryStateFlowStore
import com.ensody.reactivestate.ReactiveStateContext
import com.ensody.reactivestate.ReactiveViewModel
import com.ensody.reactivestate.invokeOnCompletion
import com.ensody.reactivestate.withSpinLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex

/**
 * Creates a multiplatform ViewModel. The [provider] should instantiate the object directly.
 *
 * You have to pass loading and error effect handlers, so the most basic functionality is taken care of.
 *
 * Instead of returning the raw ViewModel this returns a Compose [State] object containing the ViewModel.
 * This way the UI can keep up to date with a dynamically changing dependency injection graph.
 * If the ViewModel depends on a [DI] object it gets destroyed and re-created whenever that DI module is replaced.
 */
@ExperimentalReactiveStateApi
@Composable
public inline fun <reified VM : ReactiveViewModel> reactiveViewModel(
    key: String? = null,
    crossinline onError: (Throwable) -> Unit,
    crossinline provider: ReactiveStateContext.() -> VM,
): State<VM> =
    onViewModel(key = key) {
        provider().also {
            it.onInit.trigger()
        }
    }.also { viewModel ->
        LaunchedEffect(viewModel.value) {
            ContextualErrorsFlow.get(viewModel.value.scope).collect { onError(it) }
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
): State<T> {
    // TODO: Use qualifiedName once JS supports it
    val fullKey = "onViewModel:${T::class.simpleName}:$key"
    val storage = rememberSaveable<MutableMap<String, Any?>> { mutableMapOf() }
    return viewModel(viewModelStoreOwner = viewModelStoreOwner, key = fullKey) {
        WrapperViewModel { viewModelScope ->
            // The viewModelScope can't be used directly because we have to destroy and re-create the ViewModel whenever
            // the DI graph gets modified.
            var scopeRef: CoroutineScope? = null
            viewModelScope.invokeOnCompletion {
                scopeRef?.cancel()
            }
            DI.derived {
                scopeRef = scope
                ReactiveStateContext(
                    scope + ContextualValRoot() + ContextualStateFlowStore.valued { InMemoryStateFlowStore(storage) },
                    this,
                ).provider()
            }
        }
    }.value.collectAsStateWithLifecycle()
}

/** A wrapper ViewModel used to hold an arbitrary [value]. */
public class WrapperViewModel<T : Any?>(provider: (CoroutineScope) -> StateFlow<T>) : ViewModel() {
    public val value: StateFlow<T> = provider(viewModelScope)
}
