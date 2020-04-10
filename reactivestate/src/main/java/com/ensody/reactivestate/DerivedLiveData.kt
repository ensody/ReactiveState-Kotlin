package com.ensody.reactivestate

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

// TODO: Make this evaluate lazily and non-null
class DerivedLiveData<T>(scope: CoroutineScope, private val observer: AutoRunCallback<T>) :
    LiveData<T>(null) {

    init {
        scope.autoRun { value = observer() }
    }
}

fun <T> CoroutineScope.derived(observer: AutoRunCallback<T>) =
    DerivedLiveData(this, observer)

fun <T> ViewModel.derived(observer: AutoRunCallback<T>) =
    viewModelScope.derived(observer)
