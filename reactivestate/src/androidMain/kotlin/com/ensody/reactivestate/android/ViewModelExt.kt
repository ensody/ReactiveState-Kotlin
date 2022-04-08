package com.ensody.reactivestate.android

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ensody.reactivestate.ReactiveState

/**
 * Creates a `ViewModel`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.buildViewModel(crossinline provider: () -> T): Lazy<T> =
    viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = provider() as T
        }
    }

/**
 * Creates a `ViewModel`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> ComponentActivity.buildViewModel(crossinline provider: () -> T): Lazy<T> =
    viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = provider() as T
        }
    }

/**
 * Creates a `ViewModel` scoped to the `Activity`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.activityViewModel(crossinline provider: () -> T): Lazy<T> =
    activityViewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = provider() as T
        }
    }

/**
 * Creates a `ViewModel` with a [SavedStateHandleStore].
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> ComponentActivity.stateFlowViewModel(
    crossinline provider: (handle: SavedStateHandleStore) -> T,
): Lazy<T> =
    viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(SavedStateHandleStore(handle)) as T
        }
    }

/**
 * Creates a `ViewModel` with a [SavedStateHandleStore].
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.stateFlowViewModel(
    crossinline provider: (store: SavedStateHandleStore) -> T,
): Lazy<T> =
    viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(SavedStateHandleStore(handle)) as T
        }
    }

/**
 * Creates a `ViewModel` with a [SavedStateHandleStore], scoped to the `Activity`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.activityStateFlowViewModel(
    crossinline provider: (handle: SavedStateHandleStore) -> T,
): Lazy<T> =
    activityViewModels {
        object : AbstractSavedStateViewModelFactory(requireActivity(), null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(SavedStateHandleStore(handle)) as T
        }
    }

/**
 * Creates a `ViewModel` with a `SavedStateHandle`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.stateViewModel(
    crossinline provider: (handle: SavedStateHandle) -> T,
): Lazy<T> =
    viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(handle) as T
        }
    }

/**
 * Creates a `ViewModel` with a `SavedStateHandle`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> ComponentActivity.stateViewModel(
    crossinline provider: (handle: SavedStateHandle) -> T,
): Lazy<T> =
    viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(handle) as T
        }
    }

/**
 * Creates a `ViewModel` with a `SavedStateHandle`, scoped to the `Activity`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 *
 * @see [reactiveState] if you want to create multiplatform [ReactiveState] ViewModels.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.activityStateViewModel(
    crossinline provider: (handle: SavedStateHandle) -> T,
): Lazy<T> =
    activityViewModels {
        object : AbstractSavedStateViewModelFactory(requireActivity(), null) {
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(handle) as T
        }
    }
