package com.ensody.reactivestate

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewModel> Fragment.viewModel(crossinline provider: () -> T) =
    viewModels<T> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = provider() as T
        }
    }

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewModel> Fragment.activityViewModel(crossinline provider: () -> T) =
    activityViewModels<T> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = provider() as T
        }
    }

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewModel> Fragment.stateViewModel(crossinline provider: (handle: SavedStateHandle) -> T) =
    viewModels<T> {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T = provider(handle) as T
        }
    }

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewModel> Fragment.activityStateViewModel(crossinline provider: (handle: SavedStateHandle) -> T) =
    activityViewModels<T> {
        object : AbstractSavedStateViewModelFactory(requireActivity(), null) {
            override fun <T : ViewModel?> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle
            ): T = provider(handle) as T
        }
    }
