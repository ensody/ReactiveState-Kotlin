package com.ensody.reactivestate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow

public fun <T> ViewModel.derived(lazy: Boolean = false, observer: AutoRunCallback<T>): StateFlow<T> =
    viewModelScope.derived(lazy, observer)
