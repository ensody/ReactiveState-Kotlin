package com.ensody.reactivestate

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

/** Just the minimum interface needed for [Resolver]. No generic types. */
abstract class BaseAutoRunner {
    internal abstract val resolver: Resolver

    abstract fun triggerChange()
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
) : BaseAutoRunner(), Disposable {
    val listener: AutoRunOnChangeCallback<T> = onChange ?: { run() }
    override var resolver = Resolver(this)

    /** Stops watching observables. */
    override fun dispose() = observe {}

    /** Calls [observer] and tracks its dependencies. */
    fun run(): T = observe(observer)

    override fun triggerChange() {
        listener(this)
    }

    private fun <T> observe(observer: AutoRunCallback<T>): T {
        val nextResolver = Resolver(this)
        try {
            return nextResolver.observer()
        } finally {
            resolver.switchTo(nextResolver)
            resolver = nextResolver
        }
    }
}

/** Tracks observables for [AutoRunner]. */
class Resolver(val autoRunner: BaseAutoRunner) {
    private val dataToObservable = mutableMapOf<Any, AutoRunnerObservable>()
    private val observableToData = mutableMapOf<AutoRunnerObservable, Any>()

    /**
     * Tracks an arbitrary observable.
     *
     * This creates a new [AutoRunnerObservable] if one doesn't already exist for the
     * [underlyingObservable]. Otherwise it reuses the existing [AutoRunnerObservable].
     *
     * @param [underlyingObservable] The raw, underlying observable (e.g. Android's `LiveData`).
     * @param [getObservable] Used to create an [AutoRunnerObservable] wrapper around [underlyingObservable].
     */
    fun <S : Any, T : AutoRunnerObservable> track(
        underlyingObservable: S,
        getObservable: () -> T
    ): S {
        if (underlyingObservable !in dataToObservable) {
            val existing = autoRunner.resolver.dataToObservable[underlyingObservable]
            val observable = existing ?: getObservable()
            dataToObservable[underlyingObservable] = observable
            observableToData[observable] = underlyingObservable
            if (existing == null) {
                observable.addObserver()
            }
        }
        return underlyingObservable
    }

    internal fun switchTo(next: Resolver) {
        for (item in observableToData.keys - next.observableToData.keys) {
            item.removeObserver()
        }
    }
}

/**
 * Base interface for observing a hard-coded [AutoRunner] instance.
 *
 * You can use this to wrap actual observables (e.g. Android's `LiveData`).
 */
interface AutoRunnerObservable {
    val underlyingObservable: Any

    fun addObserver()
    fun removeObserver()
}
