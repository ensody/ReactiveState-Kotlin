package com.ensody.reactivestate

import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// -------------------------------------------------------------------------------------------------
// TextView
// -------------------------------------------------------------------------------------------------

/** Keeps [data] in sync with [view]`.text`. */
fun LifecycleOwner.bind(data: MutableLiveData<String>, view: TextView): Disposable =
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

/** Keeps [view]`.text` in sync with the value returned by [observer] (via [LifecycleOwner.autoRun]). */
fun LifecycleOwner.bind(view: TextView, observer: AutoRunCallback<String>): Disposable =
    autoRun {
        val value = observer()
        if (view.text.toString() != value) {
            view.text = value
        }
    }

/** Keeps [view]`.text` in sync with [data]. */
fun LifecycleOwner.bind(view: TextView, data: LiveData<String>): Disposable =
    bind(view) { get(data) ?: "" }

/** Keeps [data] and [view]`.text` in sync with each other (bidirectionally). */
fun LifecycleOwner.bindTwoWay(data: MutableLiveData<String>, view: TextView): Disposable =
    DisposableGroup().apply {
        add(bind(view, data))
        add(bind(data, view))
    }

// -------------------------------------------------------------------------------------------------
// Checkable
// -------------------------------------------------------------------------------------------------

/** Keeps [view]`.isChecked` in sync with the value returned by [observer] (via [LifecycleOwner.autoRun]). */
fun LifecycleOwner.bind(view: Checkable, observer: AutoRunCallback<Boolean>): Disposable =
    autoRun {
        val value = observer()
        if (view.isChecked != value) {
            view.isChecked = value
        }
    }

/** Keeps [view]`.isChecked` in sync with [data]. If `data.value` is null, [default] is used. */
fun LifecycleOwner.bind(view: Checkable, data: LiveData<Boolean>, default: Boolean = false): Disposable =
    bind(view) { get(data) ?: default }

// -------------------------------------------------------------------------------------------------
// CompoundButton (which is also a TextView, but we want the Checkable aspect)
// -------------------------------------------------------------------------------------------------

/** Keeps [data] in sync with [view]`.isChecked`. */
fun LifecycleOwner.bind(data: MutableLiveData<Boolean>, view: CompoundButton): Disposable =
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

/** Keeps [view]`.isChecked` in sync with the value returned by [observer] (via [LifecycleOwner.autoRun]). */
fun LifecycleOwner.bind(view: CompoundButton, observer: AutoRunCallback<Boolean>): Disposable =
    bind(view as Checkable, observer)

/** Keeps [view]`.isChecked` in sync with [data]. If `data.value` is null, [default] is used. */
fun LifecycleOwner.bind(view: CompoundButton, data: LiveData<Boolean>): Disposable =
    bind(view as Checkable, data)

/** Keeps [data] and [view]`.isChecked` in sync with each other (bidirectionally). */
fun LifecycleOwner.bindTwoWay(data: MutableLiveData<Boolean>, view: CompoundButton): Disposable =
    DisposableGroup().apply {
        add(bind(view, data))
        add(bind(data, view))
    }
