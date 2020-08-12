package com.ensody.reactivestate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle

/** Ensures [LiveData.getValue] is exactly of type [T] instead of T? */
fun <T> LiveData<T>.fixValueType() =
    LiveDataNonNullProxy(this)

/** Ensures [MutableLiveData.getValue] is exactly of type [T] instead of T? */
fun <T> MutableLiveData<T>.fixValueType() =
    MutableLiveDataNonNullProxy(this)

/** A MutableLiveData where [getValue] is exactly of type [T] instead of T? */
class MutableLiveDataNonNull<T>(value: T) : MutableLiveData<T>(value) {
    override fun getValue(): T = super.getValue()!!
}

/** Helper for getting a non-nullable [MutableLiveData]. */
fun <T> SavedStateHandle.getLiveDataNonNull(key: String, initialValue: T) =
    getLiveData(key, initialValue).apply {
        if (value == null) {
            value = initialValue
        }
    }.fixValueType()

open class LiveDataNonNullProxy<T>(private val source: LiveData<T>) : MediatorLiveData<T>() {
    private var initialized = false

    init {
        addSource(source) {
            it?.let {
                if (!initialized || value != it) {
                    value = it
                    initialized = true
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getValue(): T = (if (hasActiveObservers()) super.getValue() else source.value) as T
}

class MutableLiveDataNonNullProxy<T>(private val source: MutableLiveData<T>) :
    LiveDataNonNullProxy<T>(source) {
    override fun setValue(value: T) {
        super.setValue(value)
        source.value = value
    }
}
