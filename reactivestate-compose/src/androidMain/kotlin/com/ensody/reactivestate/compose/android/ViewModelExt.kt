package com.ensody.reactivestate.compose.android

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ensody.reactivestate.ExperimentalReactiveStateApi
import com.ensody.reactivestate.ReactiveState
import com.ensody.reactivestate.compose.ReactiveStateBuildContext
import com.ensody.reactivestate.compose.reactiveState
import kotlinx.coroutines.CoroutineScope

/**
 * Returns an existing [ViewModel] or creates a new one in the given owner (usually, a fragment or
 * an activity), defaulting to the owner provided by [LocalViewModelStoreOwner].
 *
 * The created [ViewModel] is associated with the given [viewModelStoreOwner] and will be retained
 * as long as the owner is alive (e.g. if it is an activity, until it is
 * finished or process is killed).
 *
 * @param viewModelStoreOwner The owner of the [ViewModel] that controls the scope and lifetime
 * of the returned [ViewModel]. Defaults to using [LocalViewModelStoreOwner].
 * @param key The key to use to identify the [ViewModel].
 * @param factory The [ViewModelProvider.Factory] that should be used to create the [ViewModel]
 * or null if you would like to use the default factory from the [LocalViewModelStoreOwner]
 * @return A [ViewModel] that is an instance of the given [VM] type.
 */
@ExperimentalReactiveStateApi
@Suppress("UNCHECKED_CAST")
@Composable
public inline fun <reified T : ViewModel> viewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    crossinline provider: () -> T,
): T = viewModel(
    modelClass = T::class.java,
    viewModelStoreOwner = viewModelStoreOwner,
    key = key,
    factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = provider() as T
    },
)

/**
 * Creates an object living on a wrapper [ViewModel]. This allows for building multiplatform ViewModels.
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
    crossinline provider: ReactiveStateBuildContext.() -> T,
): T {
    val fullKey = (key ?: "") + ":onViewModel:${T::class.qualifiedName}"
    return viewModel(viewModelStoreOwner = viewModelStoreOwner, key = fullKey) {
        WrapperViewModel { scope -> ReactiveStateBuildContext(scope).provider() }
    }.value
}

/** A wrapper ViewModel used to hold an arbitrary [value]. */
public class WrapperViewModel<T : Any?>(provider: (CoroutineScope) -> T) : ViewModel() {
    public val value: T = provider(viewModelScope)
}
