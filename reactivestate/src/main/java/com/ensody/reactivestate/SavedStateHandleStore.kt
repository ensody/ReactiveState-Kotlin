package com.ensody.reactivestate

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope

/** A [StateFlowStore] that wraps a `SavedStateHandle`. */
public class SavedStateHandleStore(private val scope: CoroutineScope, private val savedStateHandle: SavedStateHandle) :
    StateFlowStore {

    private val store = InMemoryStateFlowStore()

    override fun contains(key: String): Boolean =
        savedStateHandle.contains(key)

    override fun <T> getData(key: String, default: T): MutableValueFlow<T> {
        val tracked = store.contains(key)
        val data = store.getData(key, default)
        if (tracked) {
            return data
        }
        val liveData = savedStateHandle.getLiveData(key, default)
        scope.autoRun {
            data.value = get(liveData)!!
        }
        scope.autoRun {
            liveData.postValue(get(data))
        }
        return data
    }
}

public fun SavedStateHandle.stateFlowStore(scope: CoroutineScope): SavedStateHandleStore =
    SavedStateHandleStore(scope, this)
