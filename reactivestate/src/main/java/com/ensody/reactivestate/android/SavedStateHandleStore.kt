package com.ensody.reactivestate.android

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import com.ensody.reactivestate.InMemoryStateFlowStore
import com.ensody.reactivestate.MutableValueFlow
import com.ensody.reactivestate.StateFlowStore
import com.ensody.reactivestate.autoRun
import kotlinx.coroutines.CoroutineScope

/** A [StateFlowStore] that wraps a [SavedStateHandle].
 *
 * This can synchronize either
 * - two-way ([MutableValueFlow] <-> `LiveData`) if [scope] is not null
 * - one-way ([MutableValueFlow] -> `LiveData`) if [scope] is null
 *
 * Depending on whether you already have a scope
 */
public class SavedStateHandleStore(private val scope: CoroutineScope?, private val savedStateHandle: SavedStateHandle) :
    StateFlowStore {

    /** Wraps the given [SavedStateHandle] and synchronizes one-way from [MutableValueFlow] to `LiveData`. */
    public constructor(savedStateHandle: SavedStateHandle) : this(scope = null, savedStateHandle = savedStateHandle)

    private val store = InMemoryStateFlowStore()

    override fun contains(key: String): Boolean =
        savedStateHandle.contains(key)

    override fun <T> getData(key: String, default: T): MutableValueFlow<T> {
        val tracked = store.contains(key)
        if (tracked) {
            return store.getData(key, default)
        }
        val liveData = savedStateHandle.getLiveData(key, default)
        val data = store.getData(key, default) {
            liveData.postValue(it)
        }
        scope?.autoRun {
            data.value = get(liveData)!!
        }
        return data
    }
}

public fun SavedStateHandle.stateFlowStore(scope: CoroutineScope): SavedStateHandleStore =
    SavedStateHandleStore(scope, this)

/** Returns a [StateFlowStore] where you can put your saved instance state. */
public val Fragment.savedInstanceState: StateFlowStore get() =
    buildOnViewModel { stateFlowStore }.value

/** Returns a [StateFlowStore] where you can put your saved instance state. */
public val ComponentActivity.savedInstanceState: StateFlowStore get() =
    buildOnViewModel { stateFlowStore }.value
