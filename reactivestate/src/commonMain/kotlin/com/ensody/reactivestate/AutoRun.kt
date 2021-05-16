package com.ensody.reactivestate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. [CoroutineLauncher] -> UI).
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed when the [CoroutineLauncher]'s scope completes.
 *
 * See [AutoRunner] for more details.
 *
 * @param [onChange] Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param [observer] The callback which is used to track the observables.
 */
public fun CoroutineLauncher.autoRun(
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>,
): AutoRunner<Unit> =
    AutoRunner(launcher = this, onChange = onChange, observer = observer).apply {
        disposeOnCompletionOf(this@autoRun)
        run()
    }

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. [CoroutineLauncher] -> UI).
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed when the [CoroutineLauncher]'s scope completes.
 *
 * See [AutoRunner] for more details.
 *
 * @param onChange Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param flowTransformer How changes should be executed/collected. Defaults to `{ conflatedWorker() }`.
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.generalLoading].
 * @param observer The callback which is used to track the observables.
 */
public fun CoroutineLauncher.coAutoRun(
    onChange: CoAutoRunOnChangeCallback<Unit>? = null,
    flowTransformer: AutoRunFlowTransformer = defaultAutoRunFlowTransformer,
    dispatcher: CoroutineDispatcher = dispatchers.default,
    withLoading: MutableValueFlow<Int>? = generalLoading,
    observer: CoAutoRunCallback<Unit>,
): CoAutoRunner<Unit> =
    CoAutoRunner(
        launcher = this,
        onChange = onChange,
        flowTransformer = flowTransformer,
        dispatcher = dispatcher,
        withLoading = withLoading,
        observer = observer,
    ).apply {
        disposeOnCompletionOf(this@coAutoRun)
        launch { run() }
    }

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed when the [CoroutineScope] completes.
 *
 * See [AutoRunner] for more details.
 *
 * @param launcher The [CoroutineLauncher] to use.
 * @param onChange Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param observer The callback which is used to track the observables.
 */
public fun CoroutineScope.autoRun(
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    onChange: AutoRunOnChangeCallback<Unit>? = null,
    observer: AutoRunCallback<Unit>,
): AutoRunner<Unit> =
    launcher.autoRun(onChange = onChange, observer = observer)

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 *
 * This is a convenience function that immediately starts the [AutoRunner.run] cycle for you.
 *
 * Returns the underlying [AutoRunner]. To stop watching, you should call [AutoRunner.dispose].
 * The [AutoRunner] is automatically disposed when the [CoroutineScope] completes.
 *
 * See [AutoRunner] for more details.
 *
 * @param launcher The [CoroutineLauncher] to use.
 * @param onChange Gets called when the observables change. If you provide a handler you have to
 * manually call [run].
 * @param flowTransformer How changes should be executed/collected. Defaults to `{ conflatedWorker() }`.
 * @param observer The callback which is used to track the observables.
 */
public fun CoroutineScope.coAutoRun(
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    onChange: CoAutoRunOnChangeCallback<Unit>? = null,
    flowTransformer: AutoRunFlowTransformer = defaultAutoRunFlowTransformer,
    observer: CoAutoRunCallback<Unit>,
): CoAutoRunner<Unit> =
    launcher.coAutoRun(onChange = onChange, flowTransformer = flowTransformer, observer = observer)

/** Just the minimum interface needed for [Resolver]. No generic types. */
public abstract class BaseAutoRunner : AttachedDisposables {
    internal abstract val resolver: Resolver
    public abstract val launcher: CoroutineLauncher

    public abstract fun triggerChange()
}

public abstract class InternalBaseAutoRunner : BaseAutoRunner() {
    override val attachedDisposables: DisposableGroup = DisposableGroup()
    override var resolver: Resolver = Resolver(this)

    /** Stops watching observables. */
    override fun dispose() {
        resolver = Resolver(this).also {
            resolver.switchTo(it)
        }
        super.dispose()
    }
}

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 * This is the synchronous version. See [CoAutoRunner] for the suspension function based version.
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
 * @param launcher The [CoroutineLauncher] to use.
 * @param onChange Gets called when the observables change. Your onChange handler has to
 * manually call [run] at any point (e.g. asynchronously) to change the tracked observables.
 * @param observer The callback which is used to track the observables.
 */
public class AutoRunner<T>(
    override val launcher: CoroutineLauncher,
    onChange: AutoRunOnChangeCallback<T>? = null,
    private val observer: AutoRunCallback<T>,
) : InternalBaseAutoRunner() {
    public val listener: AutoRunOnChangeCallback<T> = onChange ?: { run() }

    /** Calls [observer] and tracks its dependencies. */
    public fun run(): T = observe(observer = observer)

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

/**
 * Watches observables for changes. Often useful to keep things in sync (e.g. ViewModel -> UI).
 * This is the suspension function based version. See [AutoRunner] for the synchronous version.
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
 * @param launcher The [CoroutineLauncher] to use.
 * @param onChange Gets called when the observables change. Your onChange handler has to
 * manually call [run] at any point (e.g. asynchronously) to change the tracked observables.
 * @param flowTransformer How changes should be executed/collected. Defaults to `{ conflatedWorker() }`.
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.default`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.generalLoading].
 * @param observer The callback which is used to track the observables.
 */
public class CoAutoRunner<T>(
    override val launcher: CoroutineLauncher,
    onChange: CoAutoRunOnChangeCallback<T>? = null,
    private val flowTransformer: AutoRunFlowTransformer = defaultAutoRunFlowTransformer,
    private val dispatcher: CoroutineDispatcher = dispatchers.default,
    private val withLoading: MutableValueFlow<Int>? = launcher.generalLoading,
    private val observer: CoAutoRunCallback<T>,
) : InternalBaseAutoRunner() {
    override val attachedDisposables: DisposableGroup = DisposableGroup()
    public val listener: CoAutoRunOnChangeCallback<T> = onChange ?: { run() }
    override var resolver: Resolver = Resolver(this)
    private val changeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        launcher.launch(withLoading = null) {
            changeFlow.map { suspend { worker() } }.flowTransformer().collect()
        }
    }

    private suspend fun worker() {
        // Launch a new coroutine, so the loading state can be tracked if necessary
        val job = launcher.launch(withLoading = withLoading) {
            listener(this@CoAutoRunner)
        }
        try {
            job.join()
        } catch (e: CancellationException) {
            job.cancel()
            throw e
        }
    }

    /** Calls [observer] and tracks its dependencies. */
    public suspend fun run(): T =
        withContext(dispatcher) {
            observe(observer = observer)
        }

    override fun triggerChange() {
        changeFlow.tryEmit(Unit)
    }

    private suspend fun <T> observe(observer: CoAutoRunCallback<T>): T {
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

/** Tracks observables for [AutoRunner] and [CoAutoRunner]. */
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
     *
     * @return The instantiated [AutoRunnerObservable] of type [T].
     */
    public fun <S : Any, T : AutoRunnerObservable> track(underlyingObservable: S, getObservable: () -> T): T {
        val existing = autoRunner.resolver.observables[underlyingObservable]
        @Suppress("UNCHECKED_CAST")
        val castExisting = existing as? T
        val observable = castExisting ?: getObservable()
        observables[underlyingObservable] = observable
        if (castExisting == null) {
            observable.addObserver()
            existing?.removeObserver()
        }
        return observable
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

/** The default [CoAutoRunner]/[derived] flow transformer which is: `{ conflatedWorker() }` */
public val defaultAutoRunFlowTransformer: AutoRunFlowTransformer = { conflatedWorker() }
