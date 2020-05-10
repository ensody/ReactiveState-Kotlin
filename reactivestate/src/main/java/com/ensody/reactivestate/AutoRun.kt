package com.ensody.reactivestate

import androidx.lifecycle.*

/**
 * Watches observables for changes. Often useful to keep things in sync.
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed when the [viewModelScope] completes.
 *
 * See [AutoRunner] for more details.
 *
 * @param [onChange] Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param [observer] The callback which is used to track the observables.
 */
fun ViewModel.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>
) = viewModelScope.autoRun(onChange, observer)

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 *
 * IMPORTANT: Unlike the other `autoRun` variants this only runs between a single `onStart`/`onStop`
 * lifecycle. This is safe for use in `Fragment.onStart()`.
 * Use `lifecycleScope.autoRun` to observe during the whole object lifetime, instead.
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed when the `Livecycle` stops.
 *
 * See [AutoRunner] for more details.
 *
 * @param [onChange] Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param [observer] The callback which is used to track the observables.
 */
fun LifecycleOwner.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>
): AutoRunner<Unit> {
    var hasOnStop = false
    val clearHasOnStop = OnDispose { hasOnStop = false }
    val autoRunner = AutoRunner(lifecycleScope, onChange) {
        if (!hasOnStop) {
            hasOnStop = true
            autoRunner.attachedDisposables.apply {
                add(clearHasOnStop)
                add(onStopOnce { autoRunner.dispose() })
            }
        }
        observer()
    }
    autoRunner.attachedDisposables.add(onStartOnce { autoRunner.run() })
    return autoRunner
}

/** Returns [LiveData.getValue] and tracks the observable. */
fun <T> Resolver.get(data: MutableLiveDataNonNull<T>): T = track(data).value

/** Returns [LiveData.getValue] and tracks the observable. */
fun <T> Resolver.get(data: DerivedLiveData<T>): T = track(data).value

/** Returns [LiveData.getValue] and tracks the observable. */
fun <T> Resolver.get(data: LiveDataNonNullProxy<T>): T = track(data).value

/** Returns [LiveData.getValue] and tracks the observable. */
fun <T> Resolver.get(data: LiveData<T>): T? = track(data).value

private fun <T, D : LiveData<T>> Resolver.track(data: D): D =
    track(data) { LiveDataObservable(data, autoRunner) }

private class LiveDataObservable(
    private val data: LiveData<*>,
    autoRunner: BaseAutoRunner
) : AutoRunnerObservable {
    private var ignoreFirst = false
    private val observer = Observer<Any> {
        if (ignoreFirst) {
            ignoreFirst = false
        } else {
            autoRunner.triggerChange()
        }
    }

    override fun addObserver() {
        // Prevent recursion and assume the value is already set correctly
        ignoreFirst = !data.hasActiveObservers()
        data.observeForever(observer)
    }

    override fun removeObserver() {
        data.removeObserver(observer)
    }
}
