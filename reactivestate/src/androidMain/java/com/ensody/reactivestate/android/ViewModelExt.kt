package com.ensody.reactivestate.android

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.ensody.reactivestate.NamespacedStateFlowStore
import com.ensody.reactivestate.StateFlowStore
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

/**
 * Creates an object living on a wrapper `ViewModel`. This allows for building multiplatform ViewModels.
 *
 * The [provider] should instantiate the object directly.
 *
 * @see [buildViewModel] and [stateViewModel] if you want to instantiate an Android `ViewModel` directly.
 */
public inline fun <reified T : Any> Fragment.buildOnViewModel(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> =
    stateFlowViewModel { WrapperViewModel(it) }.buildOnViewModel(provider)

/**
 * Creates an object living on a wrapper `ViewModel`. This allows for building multiplatform ViewModels.
 *
 * The [provider] should instantiate the object directly.
 *
 * @see [buildViewModel] and [stateViewModel] if you want to instantiate an Android `ViewModel` directly.
 */
public inline fun <reified T : Any> ComponentActivity.buildOnViewModel(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> =
    stateFlowViewModel { WrapperViewModel(it) }.buildOnViewModel(provider)

/** Build context for [buildOnViewModel]. */
public class BuildOnViewModelContext(
    /** The [ViewModel.viewModelScope]. */
    public val scope: CoroutineScope,

    /** A [StateFlowStore] where you can store/load the saved instance state (similar to a [SavedStateHandle]). */
    public val stateFlowStore: StateFlowStore,
)

/** The wrapper ViewModel used by [buildOnViewModel]. */
public class WrapperViewModel(public val stateFlowStore: StateFlowStore) : ViewModel() {
    public val registry: MutableMap<KClass<*>, Any> = mutableMapOf()
}

/** Used internally by [buildOnViewModel]. */
public inline fun <reified T : Any> Lazy<WrapperViewModel>.buildOnViewModel(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> = lazy {
    val stateFlowStore = NamespacedStateFlowStore(
        store = value.stateFlowStore,
        namespace = T::class.qualifiedName
            ?: throw IllegalArgumentException("The class must have a qualifiedName"),
    )
    val result = value.registry[T::class] as? T
        ?: BuildOnViewModelContext(scope = value.viewModelScope, stateFlowStore = stateFlowStore).provider()
    value.registry[T::class] = result
    result
}

/**
 * Creates `ViewModel`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.buildViewModel(crossinline provider: () -> T): Lazy<T> =
    viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = provider() as T
        }
    }

/**
 * Creates `ViewModel`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> ComponentActivity.buildViewModel(crossinline provider: () -> T): Lazy<T> =
    viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = provider() as T
        }
    }

/**
 * Creates `ViewModel` scoped to the `Activity`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.activityViewModel(crossinline provider: () -> T): Lazy<T> =
    activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = provider() as T
        }
    }

/**
 * Creates `ViewModel` with a [SavedStateHandleStore].
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> ComponentActivity.stateFlowViewModel(
    crossinline provider: (handle: SavedStateHandleStore) -> T,
): Lazy<T> =
    viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(SavedStateHandleStore(handle)) as T
        }
    }

/**
 * Creates `ViewModel` with a [SavedStateHandleStore].
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.stateFlowViewModel(
    crossinline provider: (store: SavedStateHandleStore) -> T,
): Lazy<T> =
    viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(SavedStateHandleStore(handle)) as T
        }
    }

/**
 * Creates `ViewModel` with a [SavedStateHandleStore], scoped to the `Activity`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.activityStateFlowViewModel(
    crossinline provider: (handle: SavedStateHandleStore) -> T,
): Lazy<T> =
    activityViewModels {
        object : AbstractSavedStateViewModelFactory(requireActivity(), null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(SavedStateHandleStore(handle)) as T
        }
    }

/**
 * Creates `ViewModel` with a `SavedStateHandle`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.stateViewModel(
    crossinline provider: (handle: SavedStateHandle) -> T,
): Lazy<T> =
    viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(handle) as T
        }
    }

/**
 * Creates `ViewModel` with a `SavedStateHandle`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> ComponentActivity.stateViewModel(
    crossinline provider: (handle: SavedStateHandle) -> T,
): Lazy<T> =
    viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(handle) as T
        }
    }

/**
 * Creates `ViewModel` with a `SavedStateHandle`, scoped to the `Activity`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.activityStateViewModel(
    crossinline provider: (handle: SavedStateHandle) -> T,
): Lazy<T> =
    activityViewModels {
        object : AbstractSavedStateViewModelFactory(requireActivity(), null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(handle) as T
        }
    }
