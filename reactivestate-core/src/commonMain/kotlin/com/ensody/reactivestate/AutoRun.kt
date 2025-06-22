package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.invoke

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
    AutoRunner(launcher = this, onChange = onChange, immediate = true, observer = observer).apply {
        disposeOnCompletionOf(this@autoRun)
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
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.main`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.loading].
 * @param observer The callback which is used to track the observables.
 */
public fun CoroutineLauncher.coAutoRun(
    onChange: CoAutoRunOnChangeCallback<Unit>? = null,
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.main,
    withLoading: MutableStateFlow<Int>? = loading,
    observer: CoAutoRunCallback<Unit>,
): CoAutoRunner<Unit> =
    CoAutoRunner(
        launcher = this,
        onChange = onChange,
        flowTransformer = flowTransformer,
        dispatcher = dispatcher,
        withLoading = withLoading,
        immediate = true,
        observer = observer,
    ).apply {
        disposeOnCompletionOf(this@coAutoRun)
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
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.main`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to `launcher.loading`.
 * @param observer The callback which is used to track the observables.
 */
public fun CoroutineScope.coAutoRun(
    launcher: CoroutineLauncher = SimpleCoroutineLauncher(this),
    onChange: CoAutoRunOnChangeCallback<Unit>? = null,
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    dispatcher: CoroutineDispatcher = dispatchers.main,
    withLoading: MutableStateFlow<Int>? = launcher.loading,
    observer: CoAutoRunCallback<Unit>,
): CoAutoRunner<Unit> =
    launcher.coAutoRun(
        onChange = onChange,
        flowTransformer = flowTransformer,
        dispatcher = dispatcher,
        withLoading = withLoading,
        observer = observer,
    )

/** Just the minimum interface needed for [Resolver]. No generic types. */
public abstract class BaseAutoRunner : AttachedDisposables {
    internal abstract val resolver: ResolverImpl
    public abstract val launcher: CoroutineLauncher

    public abstract fun triggerChange()

    internal abstract val isActive: Boolean
}

public abstract class InternalBaseAutoRunner(
    final override val launcher: CoroutineLauncher,
    protected val flowTransformer: AutoRunFlowTransformer,
    private val immediate: Boolean,
) : BaseAutoRunner() {
    override val attachedDisposables: DisposableGroup = DisposableGroup()
    override var resolver: ResolverImpl = ResolverImpl(this)
        internal set
    protected open val withLoading: MutableStateFlow<Int>? = null

    private val changeFlow: MutableFlow<Unit> = MutableFlow(Channel.CONFLATED)
    private var flowConsumer: Job? = null

    protected fun init() {
        if (immediate) consumeChangeFlow(initial = true)
    }

    protected fun consumeChangeFlow(initial: Boolean = false) {
        if (flowConsumer != null) {
            return
        }
        flowConsumer = launcher.launch(withLoading = null) {
            coroutineScope {
                if (initial) {
                    async {
                        changeFlow.emit(Unit)
                    }
                }
                changeFlow.flowTransformer {
                    launcher.track(withLoading = withLoading) {
                        emit(worker())
                    }
                }.collect()
            }
        }
    }

    override val isActive: Boolean get() = flowConsumer != null

    protected abstract suspend fun worker()

    override fun triggerChange() {
        changeFlow.tryEmit(Unit)
    }

    /** Stops watching observables. */
    override fun dispose() {
        flowConsumer?.cancel()
        resolver = ResolverImpl(this).also(resolver::switchTo)
        flowConsumer = null
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
 *                 manually call [run] at any point (e.g. asynchronously) to change the tracked observables.
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param immediate Whether to start tracking in a background coroutine immediately.
 * @param observer The callback which is used to track the observables.
 */
public class AutoRunner<T>(
    launcher: CoroutineLauncher,
    onChange: AutoRunOnChangeCallback<T>? = null,
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    immediate: Boolean = false,
    private val observer: AutoRunCallback<T>,
) : InternalBaseAutoRunner(launcher, flowTransformer, immediate = false) {
    private val listener: AutoRunOnChangeCallback<T> = onChange ?: { run() }

    init {
        init()
        if (immediate) run()
    }

    /** Calls [observer] and tracks its dependencies unless [once] is `true`. */
    public fun run(once: Boolean = false): T {
        if (!once) consumeChangeFlow()
        return observe(once, observer)
    }

    override suspend fun worker() {
        listener(this)
    }

    private fun <T> observe(once: Boolean, observer: AutoRunCallback<T>): T {
        if (once) return ResolverImpl(this, once).observer()

        val previousResolver = resolver
        val nextResolver = ResolverImpl(this)
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
 * @param flowTransformer How changes should be executed/collected. Defaults to [conflatedWorker].
 * @param dispatcher The [CoroutineDispatcher] to use. Defaults to `dispatchers.main`.
 * @param withLoading Tracks loading state for the (re-)computation. Defaults to [CoroutineLauncher.loading].
 * @param immediate Whether to start tracking in a background coroutine immediately.
 * @param observer The callback which is used to track the observables.
 */
public class CoAutoRunner<T>(
    launcher: CoroutineLauncher,
    onChange: CoAutoRunOnChangeCallback<T>? = null,
    flowTransformer: AutoRunFlowTransformer = { conflatedWorker(transform = it) },
    private val dispatcher: CoroutineDispatcher = dispatchers.main,
    override val withLoading: MutableStateFlow<Int>? = launcher.loading,
    immediate: Boolean = false,
    private val observer: CoAutoRunCallback<T>,
) : InternalBaseAutoRunner(launcher, flowTransformer, immediate) {
    override val attachedDisposables: DisposableGroup = DisposableGroup()
    private val listener: CoAutoRunOnChangeCallback<T> = onChange ?: { run() }
    override var resolver: ResolverImpl = ResolverImpl(this)

    init {
        init()
    }

    /** Calls [observer] and tracks its dependencies unless [once] is `true`. */
    public suspend fun run(once: Boolean = false): T =
        dispatcher {
            if (!once) consumeChangeFlow()
            observe(once, observer)
        }

    override suspend fun worker() {
        listener(this)
    }

    private suspend fun <T> observe(once: Boolean, observer: CoAutoRunCallback<T>): T {
        if (once) return ResolverImpl(this, once).observer()

        val previousResolver = resolver
        val nextResolver = ResolverImpl(this)
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
public interface Resolver {
    public val autoRunner: BaseAutoRunner

    @InternalReactiveStateApi
    public val InternalResolver.once: Boolean

    public fun <S : Any, T : AutoRunnerObservable<V>, V> track(
        underlyingObservable: S,
        getObservable: () -> T,
    ): FrozenAutoRunnerObservable<V, T>

    @InternalReactiveStateApi
    public val InternalResolver.observables: Map<Any, FrozenAutoRunnerObservable<*, *>>
}

@InternalReactiveStateApi
public object InternalResolver

@OptIn(InternalReactiveStateApi::class)
internal class ResolverImpl(override val autoRunner: BaseAutoRunner, private val once: Boolean = false) : Resolver {
    override val InternalResolver.once: Boolean
        get() = this@ResolverImpl.once
    private val observables = mutableMapOf<Any, FrozenAutoRunnerObservable<*, *>>()
    override val InternalResolver.observables: Map<Any, FrozenAutoRunnerObservable<*, *>>
        get() = this@ResolverImpl.observables

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
    override fun <S : Any, T : AutoRunnerObservable<V>, V> track(
        underlyingObservable: S,
        getObservable: () -> T,
    ): FrozenAutoRunnerObservable<V, T> {
        val existing = autoRunner.resolver.observables[underlyingObservable]

        @Suppress("UNCHECKED_CAST")
        val castExisting = existing?.observable as? T
        val observable = FrozenAutoRunnerObservable<V, T>(castExisting ?: getObservable())
        observables[underlyingObservable] = observable
        if (!once && autoRunner.isActive && castExisting == null) {
            observable.observable.addObserver(autoRunner)
            existing?.observable?.removeObserver(autoRunner)
        }
        return observable
    }

    internal fun switchTo(next: ResolverImpl) {
        for ((underlyingObservable, item) in observables) {
            if (item.observable != next.observables[underlyingObservable]?.observable) {
                item.observable.removeObserver(autoRunner)
            }
        }
    }
}

/**
 * Base interface for observing a hard-coded [AutoRunner] instance.
 *
 * You can use this to wrap actual observables (e.g. Android's `LiveData`).
 */
public interface AutoRunnerObservable<T> {
    public val value: T
    public val revisionedValue: Pair<T, ULong> get() = value to 0U
    public fun addObserver(autoRunner: BaseAutoRunner)
    public fun removeObserver(autoRunner: BaseAutoRunner)
}

public class FrozenAutoRunnerObservable<T, O : AutoRunnerObservable<T>>(
    public val observable: AutoRunnerObservable<T>,
) {
    public val value: T by lazy { revisionedValue.first }
    public val revisionedValue: Pair<T, ULong> by lazy { observable.revisionedValue }
}

/**
 * Runs [block] with a [Resolver] as the `this` argument, so you can evaluate an observer block without subscribing.
 */
public fun <T> runWithResolver(block: Resolver.() -> T): T =
    AutoRunner(scopelessCoroutineLauncher, observer = block).run(once = true)

/**
 * Runs [block] with a [Resolver] as the `this` argument, so you can evaluate an observer block without subscribing.
 */
public suspend fun <T> coRunWithResolver(block: suspend Resolver.() -> T): T =
    CoAutoRunner(scopelessCoroutineLauncher, observer = block).run(once = true)
