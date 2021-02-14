package com.ensody.reactivestate.android

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.ensody.reactivestate.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.mapLatest

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
 * @param onChange Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param observer The callback which is used to track the observables.
 */
public fun ViewModel.autoRun(
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(viewModelScope),
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>,
): AutoRunner<Unit> =
    launcher.autoRun(onChange = onChange, observer = observer)

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
 * @param onChange Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param observer The callback which is used to track the observables.
 */
public fun ViewModel.coAutoRun(
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(viewModelScope),
    onChange: CoAutoRunOnChangeCallback<Unit>? = null,
    observer: CoAutoRunCallback<Unit>,
): CoAutoRunner<Unit> =
    launcher.coAutoRun(onChange = onChange, observer = observer)

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
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(lifecycleScope),
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>,
): AutoRunner<Unit> {
    var active = false
    lateinit var autoRunner: AutoRunner<Unit>
    autoRunner = AutoRunner(launcher = launcher, onChange = onChange) {
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
 * @param flowTransformer How changes should be collected. Defaults to `{ mapLatest { } }`.
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Whether loading state may be tracked for the (re-)computation. Defaults to `true`.
 * @param [observer] The callback which is used to track the observables.
 */
public fun LifecycleOwner.coAutoRun(
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else SimpleCoroutineLauncher(lifecycleScope),
    onChange: CoAutoRunOnChangeCallback<Unit>? = null,
    flowTransformer: AutoRunFlowTransformer = { mapLatest { } },
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: Boolean = true,
    observer: AutoRunCallback<Unit>,
): CoAutoRunner<Unit> {
    var active = false
    lateinit var autoRunner: CoAutoRunner<Unit>
    autoRunner = CoAutoRunner(
        launcher = launcher,
        onChange = onChange,
        dispatcher = dispatcher,
        withLoading = withLoading,
        flowTransformer = flowTransformer,
    ) {
        if (!active) {
            active = true
            autoRunner.attachedDisposables.apply {
                add(OnDispose { active = false })
                add(
                    onStopOnce {
                        autoRunner.dispose()
                        add(onStartOnce { launcher.launch { autoRunner.run() } })
                        if (this@coAutoRun is Fragment) {
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
        attachedDisposables.add(onStartOnce { launcher.launch { autoRunner.run() } })
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
