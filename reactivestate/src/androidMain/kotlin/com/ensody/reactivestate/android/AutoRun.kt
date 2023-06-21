package com.ensody.reactivestate.android

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ensody.reactivestate.AutoRunCallback
import com.ensody.reactivestate.AutoRunFlowTransformer
import com.ensody.reactivestate.AutoRunOnChangeCallback
import com.ensody.reactivestate.AutoRunner
import com.ensody.reactivestate.CoAutoRunCallback
import com.ensody.reactivestate.CoAutoRunOnChangeCallback
import com.ensody.reactivestate.CoAutoRunner
import com.ensody.reactivestate.CoroutineLauncher
import com.ensody.reactivestate.MutableValueFlow
import com.ensody.reactivestate.OnDispose
import com.ensody.reactivestate.SimpleCoroutineLauncher
import com.ensody.reactivestate.autoRun
import com.ensody.reactivestate.coAutoRun
import com.ensody.reactivestate.conflatedWorker
import com.ensody.reactivestate.dispatchers
import kotlinx.coroutines.CoroutineDispatcher

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
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else LifecycleCoroutineLauncher(this),
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
                    },
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
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.loading] if
 *                    this is a [CoroutineLauncher] or `null` otherwise.
 * @param [observer] The callback which is used to track the observables.
 */
public fun LifecycleOwner.coAutoRun(
    launcher: CoroutineLauncher = if (this is CoroutineLauncher) this else LifecycleCoroutineLauncher(this),
    onChange: CoAutoRunOnChangeCallback<Unit>? = null,
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: MutableValueFlow<Int>? = if (this is CoroutineLauncher) launcher.loading else null,
    observer: AutoRunCallback<Unit>,
): CoAutoRunner<Unit> {
    var active = false
    lateinit var autoRunner: CoAutoRunner<Unit>
    autoRunner = CoAutoRunner(
        launcher = launcher,
        onChange = onChange,
        dispatcher = dispatcher,
        withLoading = withLoading,
        immediate = true,
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
                    },
                )
            }
        }
        observer()
    }
    return autoRunner.apply {
        attachedDisposables.add(onStartOnce { launcher.launch { autoRunner.run() } })
    }
}
