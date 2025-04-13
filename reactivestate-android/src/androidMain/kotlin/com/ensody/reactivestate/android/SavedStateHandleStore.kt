package com.ensody.reactivestate.android

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import com.ensody.reactivestate.InMemoryStateFlowStore
import com.ensody.reactivestate.StateFlowStore
import com.ensody.reactivestate.autoRun
import com.ensody.reactivestate.get
import com.ensody.reactivestate.propertyName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.ReadOnlyProperty

/** A [StateFlowStore] that wraps a [SavedStateHandle].
 *
 * This can synchronize either
 * - two-way ([MutableStateFlow] <-> `LiveData`) if [scope] is not null
 * - one-way ([MutableStateFlow] -> `LiveData`) if [scope] is null
 *
 * Depending on whether you already have a scope
 */
public class SavedStateHandleStore(private val scope: CoroutineScope?, private val savedStateHandle: SavedStateHandle) :
    StateFlowStore {

    /** Wraps the given [SavedStateHandle] and synchronizes one-way from [MutableStateFlow] to `LiveData`. */
    public constructor(savedStateHandle: SavedStateHandle) : this(scope = null, savedStateHandle = savedStateHandle)

    private val store = InMemoryStateFlowStore()

    override fun contains(key: String): Boolean =
        savedStateHandle.contains(key)

    override fun <T> getData(key: String, default: T): MutableStateFlow<T> {
        val tracked = store.contains(key)
        if (tracked) {
            return store.getData(key, default)
        }
        val liveData = savedStateHandle.getLiveData(key, default)

        @Suppress("UNCHECKED_CAST")
        val data = store.getData<T>(key, liveData.value as T) {
            liveData.postValue(it)
        }
        scope?.autoRun {
            @Suppress("UNCHECKED_CAST")
            data.value = get(liveData) as T
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

/** Returns a [StateFlowStore] where you can put your saved instance state. */
public fun <T> Fragment.savedInstanceState(default: T): ReadOnlyProperty<Any?, MutableStateFlow<T>> =
    propertyName { savedInstanceState.getData(it, default) }

/** Returns a [StateFlowStore] where you can put your saved instance state. */
public fun <T> ComponentActivity.savedInstanceState(default: T): ReadOnlyProperty<Any?, MutableStateFlow<T>> =
    propertyName { savedInstanceState.getData(it, default) }
