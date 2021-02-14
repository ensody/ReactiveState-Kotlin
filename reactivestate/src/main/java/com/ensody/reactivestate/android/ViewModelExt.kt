package com.ensody.reactivestate.android

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*

/**
 * Creates `ViewModel`.
 *
 * The [provider] should instantiate the `ViewModel` directly.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : ViewModel> Fragment.buildViewModel(crossinline provider: () -> T): Lazy<T> =
    viewModels<T> {
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
    viewModels<T> {
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
    activityViewModels<T> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = provider() as T
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
    viewModels<T> {
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
    viewModels<T> {
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
    activityViewModels<T> {
        object : AbstractSavedStateViewModelFactory(requireActivity(), null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle,
            ): T = provider(handle) as T
        }
    }
