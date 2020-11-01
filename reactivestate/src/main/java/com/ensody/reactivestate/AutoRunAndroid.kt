package com.ensody.reactivestate

import androidx.fragment.app.Fragment
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
public fun ViewModel.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>,
): AutoRunner<Unit> = viewModelScope.autoRun(onChange, observer)

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 *
 * This only executes the observer between `onStart`/`onStop`.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed on `Activity.onDestroy`/`Fragment.onDestroyView`.
 *
 * See [AutoRunner] for more details.
 *
 * @param [onChange] Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param [observer] The callback which is used to track the observables.
 */
public fun LifecycleOwner.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>,
): AutoRunner<Unit> {
    var active = false
    lateinit var autoRunner: AutoRunner<Unit>
    autoRunner = AutoRunner(lifecycleScope, onChange) {
        if (!active) {
            active = true
            autoRunner.attachedDisposables.apply {
                add(OnDispose { active = false })
                add(
                    onStopOnce {
                        autoRunner.dispose()
                        add(onStartOnce { autoRunner.run() })
                        if (this@autoRun is Fragment) {
                            add(onDestroyView { autoRunner.dispose() })
                        } else {
                            add(onDestroy { autoRunner.dispose() })
                        }
                    }
                )
            }
        }
        observer()
    }
    return autoRunner.apply {
        attachedDisposables.add(onStartOnce { autoRunner.run() })
    }
}

/** Returns [LiveData.getValue] and tracks the observable. */
public fun <T> Resolver.get(data: LiveData<T>): T? = track(data).value

private fun <T, D : LiveData<T>> Resolver.track(data: D): D =
    track(data) { LiveDataObservable(data, autoRunner) }

private class LiveDataObservable(
    private val data: LiveData<*>,
    autoRunner: BaseAutoRunner,
) : AutoRunnerObservable {
    private var ignore = false
    private val observer = Observer<Any> {
        if (!ignore) {
            autoRunner.triggerChange()
        }
    }

    override fun addObserver() {
        // Prevent recursion and assume the value is already set correctly
        ignore = true
        data.observeForever(observer)
        ignore = false
    }

    override fun removeObserver() {
        data.removeObserver(observer)
    }
}
