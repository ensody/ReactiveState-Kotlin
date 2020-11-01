package com.ensody.reactivestate

import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// -------------------------------------------------------------------------------------------------
// TextView
// -------------------------------------------------------------------------------------------------

/** Keeps [data] in sync with [view]`.text`. */
public fun LifecycleOwner.bind(data: MutableLiveData<String>, view: TextView): Disposable =
    DisposableGroup().apply {
        add(
            onStartOnce {
                val watcher = view.addTextChangedListener {
                    val value = view.text.toString()
                    if (data.value != value) {
                        data.value = value
                    }
                }
                add(OnDispose { view.removeTextChangedListener(watcher) })
                add(onStopOnce { dispose() })
            }
        )
    }

/** Keeps [data] in sync with [view]`.text`. */
public fun LifecycleOwner.bind(data: MutableStateFlow<String>, view: TextView): Disposable =
    DisposableGroup().apply {
        add(
            onStartOnce {
                val watcher = view.addTextChangedListener {
                    val value = view.text.toString()
                    if (data.value != value) {
                        data.value = value
                    }
                }
                add(OnDispose { view.removeTextChangedListener(watcher) })
                add(onStopOnce { dispose() })
            }
        )
    }

/** Keeps [view]`.text` in sync with the value returned by [observer] (via [LifecycleOwner.autoRun]). */
public fun LifecycleOwner.bind(view: TextView, observer: AutoRunCallback<String>): Disposable =
    autoRun {
        val value = observer()
        if (view.text.toString() != value) {
            view.text = value
        }
    }

/** Keeps [view]`.text` in sync with [data]. */
public fun LifecycleOwner.bind(view: TextView, data: LiveData<String>): Disposable =
    bind(view) { get(data) ?: "" }

/** Keeps [view]`.text` in sync with [data]. */
public fun LifecycleOwner.bind(view: TextView, data: StateFlow<String>): Disposable =
    bind(view) { get(data) }

/** Keeps [data] and [view]`.text` in sync with each other (bidirectionally). */
public fun LifecycleOwner.bindTwoWay(data: MutableLiveData<String>, view: TextView): Disposable =
    DisposableGroup().apply {
        add(bind(view, data))
        add(bind(data, view))
    }

/** Keeps [data] and [view]`.text` in sync with each other (bidirectionally). */
public fun LifecycleOwner.bindTwoWay(data: MutableStateFlow<String>, view: TextView): Disposable =
    DisposableGroup().apply {
        add(bind(view, data))
        add(bind(data, view))
    }

// -------------------------------------------------------------------------------------------------
// Checkable
// -------------------------------------------------------------------------------------------------

/** Keeps [view]`.isChecked` in sync with the value returned by [observer] (via [LifecycleOwner.autoRun]). */
public fun LifecycleOwner.bind(view: Checkable, observer: AutoRunCallback<Boolean>): Disposable =
    autoRun {
        val value = observer()
        if (view.isChecked != value) {
            view.isChecked = value
        }
    }

/** Keeps [view]`.isChecked` in sync with [data]. If `data.value` is null, [default] is used. */
public fun LifecycleOwner.bind(view: Checkable, data: LiveData<Boolean>, default: Boolean = false): Disposable =
    bind(view) { get(data) ?: default }

/** Keeps [view]`.isChecked` in sync with [data]. If `data.value` is null, [default] is used. */
public fun LifecycleOwner.bind(view: Checkable, data: StateFlow<Boolean>): Disposable =
    bind(view) { get(data) }

// -------------------------------------------------------------------------------------------------
// CompoundButton (which is also a TextView, but we want the Checkable aspect)
// -------------------------------------------------------------------------------------------------

/** Keeps [data] in sync with [view]`.isChecked`. */
public fun LifecycleOwner.bind(data: MutableLiveData<Boolean>, view: CompoundButton): Disposable =
    DisposableGroup().apply {
        add(
            onStartOnce {
                view.setOnCheckedChangeListener { _, isChecked ->
                    if (data.value != isChecked) {
                        data.value = isChecked
                    }
                }
                add(OnDispose { view.setOnCheckedChangeListener(null) })
                add(onStopOnce { dispose() })
            }
        )
    }

/** Keeps [data] in sync with [view]`.isChecked`. */
public fun LifecycleOwner.bind(data: MutableStateFlow<Boolean>, view: CompoundButton): Disposable =
    DisposableGroup().apply {
        add(
            onStartOnce {
                view.setOnCheckedChangeListener { _, isChecked ->
                    if (data.value != isChecked) {
                        data.value = isChecked
                    }
                }
                add(OnDispose { view.setOnCheckedChangeListener(null) })
                add(onStopOnce { dispose() })
            }
        )
    }

/** Keeps [view]`.isChecked` in sync with the value returned by [observer] (via [LifecycleOwner.autoRun]). */
public fun LifecycleOwner.bind(view: CompoundButton, observer: AutoRunCallback<Boolean>): Disposable =
    bind(view as Checkable, observer)

/** Keeps [view]`.isChecked` in sync with [data]. If `data.value` is null, [default] is used. */
public fun LifecycleOwner.bind(view: CompoundButton, data: LiveData<Boolean>, default: Boolean = false): Disposable =
    bind(view as Checkable, data, default = default)

/** Keeps [view]`.isChecked` in sync with [data] */
public fun LifecycleOwner.bind(view: CompoundButton, data: StateFlow<Boolean>): Disposable =
    bind(view as Checkable, data)

/** Keeps [data] and [view]`.isChecked` in sync with each other (bidirectionally). */
public fun LifecycleOwner.bindTwoWay(data: MutableLiveData<Boolean>, view: CompoundButton): Disposable =
    DisposableGroup().apply {
        add(bind(view, data))
        add(bind(data, view))
    }

/** Keeps [data] and [view]`.isChecked` in sync with each other (bidirectionally). */
public fun LifecycleOwner.bindTwoWay(data: MutableStateFlow<Boolean>, view: CompoundButton): Disposable =
    DisposableGroup().apply {
        add(bind(view, data))
        add(bind(data, view))
    }
