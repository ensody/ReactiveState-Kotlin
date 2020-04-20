package com.ensody.reactivestate

import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

private typealias Scope = LifecycleOwner

// -------------------------------------------------------------------------------------------------
// TextView
// -------------------------------------------------------------------------------------------------

fun Scope.bind(data: MutableLiveData<String>, view: TextView): Disposable =
    DisposableGroup().apply {
        add(onStartOnce {
            val watcher = view.addTextChangedListener {
                val value = view.text.toString()
                if (data.value != value) {
                    data.value = value
                }
            }
            add(object : Disposable {
                override fun dispose() {
                    view.removeTextChangedListener(watcher)
                }
            })
            add(onStopOnce {
                dispose()
            })
        })
    }

fun Scope.bind(view: TextView, observer: AutoRunCallback<String>): Disposable =
    autoRun {
        val value = observer()
        if (view.text.toString() != value) {
            view.text = value
        }
    }

fun Scope.bind(view: TextView, data: LiveData<String>): Disposable =
    bind(view) { get(data) ?: "" }

fun Scope.bindTwoWay(data: MutableLiveData<String>, view: TextView): Disposable =
    DisposableGroup().apply {
        add(bind(view, data))
        add(bind(data, view))
    }

// -------------------------------------------------------------------------------------------------
// Checkable
// -------------------------------------------------------------------------------------------------

fun Scope.bind(view: Checkable, observer: AutoRunCallback<Boolean>): Disposable =
    autoRun {
        val value = observer()
        if (view.isChecked != value) {
            view.isChecked = value
        }
    }

fun Scope.bind(view: Checkable, data: LiveData<Boolean>, default: Boolean = false): Disposable =
    bind(view) { get(data) ?: default }

// -------------------------------------------------------------------------------------------------
// CompoundButton (which is also a TextView, but we want the Checkable aspect)
// -------------------------------------------------------------------------------------------------

fun Scope.bind(data: MutableLiveData<Boolean>, view: CompoundButton): Disposable =
    DisposableGroup().apply {
        add(onStartOnce {
            view.setOnCheckedChangeListener { _, isChecked ->
                if (data.value != isChecked) {
                    data.value = isChecked
                }
            }
            add(object : Disposable {
                override fun dispose() {
                    view.setOnCheckedChangeListener(null)
                }
            })
            add(onStopOnce {
                dispose()
            })
        })
    }

fun Scope.bind(view: CompoundButton, observer: AutoRunCallback<Boolean>): Disposable =
    bind(view as Checkable, observer)

fun Scope.bind(view: CompoundButton, data: LiveData<Boolean>): Disposable =
    bind(view as Checkable, data)

fun Scope.bindTwoWay(data: MutableLiveData<Boolean>, view: CompoundButton): Disposable =
    DisposableGroup().apply {
        add(bind(view, data))
        add(bind(data, view))
    }
