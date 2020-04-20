package com.ensody.reactivestate

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** Observer callback used by [autoRun] and [AutoRunner]. */
typealias AutoRunCallback<T> = Resolver.() -> T

/** onChange callback used by [autoRun] and [AutoRunner]. */
typealias AutoRunOnChangeCallback<T> = (AutoRunner<T>) -> Unit

private fun CoroutineContext.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>
): AutoRunner<Unit> = AutoRunner(onChange, observer).apply {
    disposeOnCompletionOf(this@autoRun)
    run()
}

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 *
 * See [AutoRunner] for more details.
 *
 * @param [onChange] Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param [observer] The callback which is used to track the observables.
 */
suspend fun autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>
): AutoRunner<Unit> =
    coroutineContext.autoRun(onChange, observer)

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 *
 * See [AutoRunner] for more details.
 *
 * @param [onChange] Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param [observer] The callback which is used to track the observables.
 */
fun CoroutineScope.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>
): AutoRunner<Unit> =
    coroutineContext.autoRun(onChange, observer)

/**
 * Watches observables for changes. Often useful to keep things in sync.
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
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
 * IMPORTANT: Unlike the other autoRun variants this only runs between a single onStart/onStop
 * lifecycle. This is safe for use in Fragment.onStart().
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
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
    val autoRunner = AutoRunner(onChange, observer)
    onStartOnce { autoRunner.run() }
    onStopOnce { autoRunner.dispose() }
    return autoRunner
}

/** Just the minimum interface needed for [Resolver]. No generic types. */
interface BaseAutoRunner {
    fun addObservable(data: LiveData<*>)
}

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 *
 * Given an [observer], this class will automatically register itself as a listener and keep track
 * of the observables which [observer] depends on.
 *
 * You have to call [run] once to start watching.
 *
 * To stop watching, you should call [dispose].
 *
 * @param [onChange] Gets called when the observables change. Your onChange handler has to
 * manually call [run] at any point (e.g. asynchronously) to change the tracked observables.
 * @param [observer] The callback which is used to track the observables.
 */
class AutoRunner<T>(
    onChange: AutoRunOnChangeCallback<T>? = null,
    private val observer: AutoRunCallback<T>
) : BaseAutoRunner, Disposable {
    val listener: AutoRunOnChangeCallback<T> = onChange ?: { run() }
    private val listenerObserver = Observer<Any> {
        listener(this)
    }
    private var resolver = Resolver(this)

    /** Stops watching observables. */
    override fun dispose() = observe {}

    /** Calls [observer] and tracks its dependencies. */
    fun run(): T = observe(observer)

    private fun <T> observe(observer: AutoRunCallback<T>): T {
        val nextResolver = Resolver(this)
        try {
            return nextResolver.observer()
        } finally {
            for (item in resolver.observables - nextResolver.observables) {
                item.removeObserver(listenerObserver)
            }
            resolver = nextResolver
        }
    }

    override fun addObservable(data: LiveData<*>) {
        if (!resolver.observables.contains(data)) {
            data.observeForever(listenerObserver)
        }
    }
}

/** Tracks observables for [AutoRunner]. */
class Resolver(private val autoRunner: BaseAutoRunner) {
    internal val observables = mutableSetOf<LiveData<*>>()

    private fun <T : LiveData<*>> add(data: T): T {
        if (observables.add(data)) {
            autoRunner.addObservable(data)
        }
        return data
    }

    /** Returns [LiveData.getValue] and tracks the observable. */
    operator fun <T> get(data: MutableLiveDataNonNull<T>): T = add(data).value

    /** Returns [LiveData.getValue] and tracks the observable. */
    operator fun <T> get(data: DerivedLiveData<T>): T = add(data).value

    /** Returns [LiveData.getValue] and tracks the observable. */
    operator fun <T> get(data: LiveDataNonNullProxy<T>): T = add(data).value

    /** Returns [LiveData.getValue] and tracks the observable. */
    operator fun <T> get(data: LiveData<T>): T? = add(data).value
}
