package com.ensody.reactivestate

import android.widget.CheckBox
import android.widget.Checkable
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.*

private typealias Scope = LifecycleOwner

fun <T> Scope.bindConverted(
    data: MutableLiveData<T>,
    view: TextView,
    convert: (String) -> T
): Disposable {
    val disposable = DisposableGroup()
    disposable.add(onStartOnce {
        val watcher = view.addTextChangedListener {
            val value = convert(view.text.toString())
            if (data.value != value) {
                data.value = value
            }
        }
        disposable.add(object : Disposable {
            override fun dispose() {
                view.removeTextChangedListener(watcher)
            }
        })
        disposable.add(onStopOnce {
            disposable.dispose()
        })
    })
    return disposable
}

fun Scope.bind(
    data: MutableLiveData<String>,
    view: TextView,
    convert: (String) -> String = { it }
): Disposable =
    bindConverted(data, view, convert)

fun Scope.bind(view: TextView, observer: AutoRunCallback<String>): Disposable =
    autoRun { get ->
        val value = observer(get)
        if (view.text.toString() != value) {
            view.text = value
        }
    }

fun <T> Scope.bind(
    view: TextView,
    data: LiveData<T>,
    convert: (T) -> String = { it.toString() }
): Disposable =
    bind(view) { get ->
        convert(get(data))
    }

fun <T> Scope.bindTwoWay(
    data: MutableLiveData<T>,
    view: TextView,
    convertToView: (T) -> String = { it.toString() },
    convertToData: (String) -> T
): Disposable =
    DisposableGroup().apply {
        add(bind(view, data, convertToView))
        add(bindConverted(data, view, convertToData))
    }

fun Scope.bindTwoWay(data: MutableLiveData<String>, view: TextView): Disposable =
    bindTwoWay(data, view) { it }

fun Scope.bindTwoWayInt(data: MutableLiveData<Int>, view: TextView): Disposable =
    bindTwoWay(data, view) { it.toInt() }

fun Scope.bindTwoWayFloat(data: MutableLiveData<Float>, view: TextView): Disposable =
    bindTwoWay(data, view) { it.toFloat() }

fun Scope.bindTwoWayDouble(data: MutableLiveData<Double>, view: TextView): Disposable =
    bindTwoWay(data, view) { it.toDouble() }

fun Scope.bind(view: Checkable, observer: AutoRunCallback<Boolean>): Disposable =
    autoRun { get ->
        val value = observer(get)
        if (view.isChecked != value) {
            view.isChecked = value
        }
    }

fun <T> Scope.bindConverted(
    view: Checkable,
    data: LiveData<T>,
    convert: (T) -> Boolean
): Disposable =
    bind(view) { get ->
        convert(get(data))
    }

fun Scope.bind(
    view: Checkable,
    data: LiveData<Boolean>,
    convert: (Boolean) -> Boolean = { it }
): Disposable =
    bind(view) { get ->
        convert(get(data))
    }

fun <T> Scope.bindConverted(
    data: MutableLiveData<T>,
    view: CheckBox,
    convert: (Boolean) -> T
): Disposable {
    val disposable = DisposableGroup()
    disposable.add(onStartOnce {
        view.setOnCheckedChangeListener { _, isChecked ->
            val value = convert(isChecked)
            if (data.value != value) {
                data.value = value
            }
        }
        disposable.add(object : Disposable {
            override fun dispose() {
                view.setOnCheckedChangeListener(null)
            }
        })
        disposable.add(onStopOnce {
            disposable.dispose()
        })
    })
    return disposable
}

fun Scope.bind(
    data: MutableLiveData<Boolean>,
    view: CheckBox,
    convert: (Boolean) -> Boolean = { it }
): Disposable =
    bindConverted(data, view, convert)

fun Scope.bind(view: CheckBox, observer: AutoRunCallback<Boolean>): Disposable =
    bind(view as Checkable, observer)

fun Scope.bind(view: CheckBox, data: LiveData<Boolean>): Disposable =
    bind(view as Checkable, data)

fun <T> Scope.bindTwoWay(
    data: MutableLiveData<T>,
    view: CheckBox,
    convertToView: (T) -> Boolean,
    convertToData: (Boolean) -> T
): Disposable =
    DisposableGroup().apply {
        add(bindConverted(view as Checkable, data, convertToView))
        add(bindConverted(data, view, convertToData))
    }
