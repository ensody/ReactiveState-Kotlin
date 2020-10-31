package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope

/** Observer callback used by [autoRun] and [AutoRunner]. */
public typealias AutoRunCallback<T> = Resolver.() -> T

/** onChange callback used by [autoRun] and [AutoRunner]. */
public typealias AutoRunOnChangeCallback<T> = (AutoRunner<T>) -> Unit

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. [CoroutineScopeOwner] -> UI).
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed when the [CoroutineScope] completes.
 *
 * See [AutoRunner] for more details.
 *
 * @param [onChange] Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param [observer] The callback which is used to track the observables.
 */
public fun CoroutineScope.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>
): AutoRunner<Unit> =
    AutoRunner(this, onChange, observer).apply {
        disposeOnCompletionOf(this@autoRun)
        run()
    }

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. [CoroutineScopeOwner] -> UI).
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed when the [CoroutineScopeOwner.scope] completes.
 *
 * See [AutoRunner] for more details.
 *
 * @param [onChange] Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param [observer] The callback which is used to track the observables.
 */
public fun CoroutineScopeOwner.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>
): AutoRunner<Unit> =
    scope.autoRun(onChange, observer)

/** Just the minimum interface needed for [Resolver]. No generic types. */
public abstract class BaseAutoRunner : AttachedDisposables {
    internal abstract val resolver: Resolver

    public abstract val autoRunnerScope: CoroutineScope
    public abstract fun triggerChange()
}

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. [CoroutineScopeOwner] -> UI).
 *
 * Given an [observer], this class will automatically register itself as a listener and keep track
 * of the observables which [observer] depends on.
 *
 * You have to call [run] once to start watching.
 *
 * To stop watching, you should call [dispose].
 *
 * Instead of instantiating an `AutoRunner` directly you'll usually want to use an [autoRun] helper.
 *
 * @param [onChange] Gets called when the observables change. Your onChange handler has to
 * manually call [run] at any point (e.g. asynchronously) to change the tracked observables.
 * @param [observer] The callback which is used to track the observables.
 */
public class AutoRunner<T>(
    override val autoRunnerScope: CoroutineScope,
    onChange: AutoRunOnChangeCallback<T>? = null,
    private val observer: AutoRunCallback<T>
) : BaseAutoRunner() {
    override val attachedDisposables: DisposableGroup = DisposableGroup()
    public val listener: AutoRunOnChangeCallback<T> = onChange ?: { run() }
    override var resolver: Resolver = Resolver(this)

    /** Stops watching observables. */
    override fun dispose() {
        observe {}
        attachedDisposables.dispose()
    }

    /** Calls [observer] and tracks its dependencies. */
    public fun run(): T = observe(observer)

    override fun triggerChange() {
        listener(this)
    }

    private fun <T> observe(observer: AutoRunCallback<T>): T {
        val previousResolver = resolver
        val nextResolver = Resolver(this)
        try {
            return nextResolver.observer()
        } finally {
            // Detect if we had a recursion (e.g. due to dispose() being called within observer)
            if (resolver === previousResolver) {
                resolver.switchTo(nextResolver)
                resolver = nextResolver
            } else {
                // The resolver has changed in the meantime, so nextResolver is outdated.
                nextResolver.switchTo(resolver)
            }
        }
    }
}

/** Tracks observables for [AutoRunner]. */
public class Resolver(public val autoRunner: BaseAutoRunner) {
    private val observables = mutableMapOf<Any, AutoRunnerObservable>()

    /**
     * Tracks an arbitrary observable.
     *
     * This creates a new [AutoRunnerObservable] if one doesn't already exist for the
     * [underlyingObservable]. Otherwise it reuses the existing [AutoRunnerObservable].
     *
     * @param [underlyingObservable] The raw, underlying observable (e.g. Android's `LiveData`).
     * @param [getObservable] Used to create an [AutoRunnerObservable] wrapper around [underlyingObservable].
     */
    public fun <S : Any, T : AutoRunnerObservable> track(
        underlyingObservable: S,
        getObservable: () -> T
    ): S {
        if (underlyingObservable !in observables) {
            val existing = autoRunner.resolver.observables[underlyingObservable]
            val observable = existing ?: getObservable()
            observables[underlyingObservable] = observable
            if (existing == null) {
                observable.addObserver()
            }
        }
        return underlyingObservable
    }

    internal fun switchTo(next: Resolver) {
        for ((underlyingObservable, item) in observables) {
            if (item != next.observables[underlyingObservable]) {
                item.removeObserver()
            }
        }
    }
}

/**
 * Base interface for observing a hard-coded [AutoRunner] instance.
 *
 * You can use this to wrap actual observables (e.g. Android's `LiveData`).
 */
public interface AutoRunnerObservable {
    public fun addObserver()
    public fun removeObserver()
}
