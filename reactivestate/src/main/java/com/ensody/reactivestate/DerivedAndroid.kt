package com.ensody.reactivestate

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

public fun <T> ViewModel.derived(started: SharingStarted, observer: AutoRunCallback<T>): StateFlow<T> =
    viewModelScope.derived(started = started, observer = observer)

public fun <T> LifecycleOwner.derived(started: SharingStarted, observer: AutoRunCallback<T>): StateFlow<T> =
    lifecycleScope.derived(started = started, observer = observer)
