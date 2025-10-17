package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.JsName

/**
 * A mechanism for [ReactiveViewModel] initialization-time tasks like repository cache refresh.
 *
 * With [observe] you get notified when the ViewModel is ready. On the provided [OnInitContext] you can launch
 * coroutines which are associated with your observer. If any of the coroutines fail the [state] will reflect that
 * and you can run [trigger] again for only the failing observers.
 */
@ExperimentalReactiveStateApi
public interface OnInit {
    /**
     * The initialization state.
     *
     * On ViewModel initialization the [trigger] function is called and any errors happening during initialization
     * will cause the state to become [State.Error]. In that case you can call [trigger] again to restart processing
     * only the failed observers.
     */
    public val state: StateFlow<State>

    public fun observe(block: OnInitContext.() -> Unit)
    public fun unobserve(block: OnInitContext.() -> Unit)

    /** Starts any not yet finished observers. This can also be used for retry if some observer fails. */
    public fun trigger()

    /** Marks all completed observers as not-yet-complete, so [trigger] will run them again. */
    public fun setInitializingState()

    public sealed interface State {
        public data object Initializing : State
        public data class Error(val errors: List<Throwable>) : State
        public data object Finished : State
    }
}

/** Triggers all observers, even completed ones, from scratch. */
public fun OnInit.retrigger() {
    setInitializingState()
    trigger()
}

/**
 * When the ViewModel is ready this API allows launching coroutines.
 *
 * This object gets passed to the observer registered via [OnInit.observe].
 */
@ExperimentalReactiveStateApi
public interface OnInitContext {
    /**
     * Launcher for tasks that will terminate (e.g. initial network requests, cache refresh, ...).
     *
     * Any errors happening here will be tracked and turn [OnInit.state] into the [OnInit.State.Error] state.
     */
    public val oneShot: CoroutineLauncher

    /** The underlying ViewModel in case you might need its [CoroutineLauncher.scope] for [ContextualVal] resolution. */
    public val source: CoroutineLauncher

    /**
     * Launches a coroutine. Mark long-running coroutines by setting [longRunning] to `true`.
     *
     * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
     * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
     * @param withLoading Tracks loading state for the (re-)computation.
     *                    Defaults to `null` since the OnInit already has the loading state.
     * @param onError Optional custom error handler.
     * @param block the coroutine code which will be invoked in the context of the provided scope.
     */
    public fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        longRunning: Boolean = false,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        withLoading: MutableStateFlow<Int>? = null,
        onError: (suspend (Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job =
        (if (longRunning) source else oneShot).launch(
            context = context,
            start = start,
            withLoading = withLoading,
            onError = onError,
            block = block,
        )
}

@JsName("createOnInit")
@ExperimentalReactiveStateApi
public fun OnInit(source: CoroutineLauncher): OnInit =
    OnInitImpl(source)

private class OnInitImpl(private val source: CoroutineLauncher) : OnInit {
    private val observers: MutableStateFlow<List<OnInitContext.() -> Unit>> = MutableStateFlow(emptyList())
    private val finishedObservers: MutableStateFlow<List<OnInitContext.() -> Unit>> = MutableStateFlow(emptyList())
    private val mutex = Mutex()

    override val state: MutableStateFlow<OnInit.State> = MutableStateFlow(OnInit.State.Initializing)

    override fun observe(block: OnInitContext.() -> Unit) {
        observers.replace { plus(block) }
    }

    override fun unobserve(block: OnInitContext.() -> Unit) {
        observers.replace { minus(block) }
        finishedObservers.replace { minus(block) }
    }

    override fun trigger() {
        source.launch(withLoading = null) {
            mutex.withLock {
                if (observers.value.isNotEmpty()) {
                    state.value = OnInit.State.Initializing
                    coroutineScope {
                        observers.value.map {
                            async {
                                val context = OnInitContextImpl(source, it)
                                context.process()
                                if (context.errors.value.isEmpty()) {
                                    observers.replace { minus(context.observer) }
                                    finishedObservers.replace { plus(context.observer) }
                                } else {
                                    state.update {
                                        val errors = (it as? OnInit.State.Error)?.errors.orEmpty()
                                        OnInit.State.Error(context.errors.value + errors)
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                }
                if (observers.value.isEmpty()) {
                    state.value = OnInit.State.Finished
                }
            }
        }
    }

    override fun setInitializingState() {
        observers.replace { plus(observers.replaceAndGet { emptyList() }) }
        state.value = OnInit.State.Initializing
    }
}

private class OnInitContextImpl(
    override val source: CoroutineLauncher,
    val observer: OnInitContext.() -> Unit,
) : OnInitContext {
    val oneShotJobs: MutableStateFlow<Int> = MutableStateFlow(0)
    val errors: MutableStateFlow<List<Throwable>> = MutableStateFlow(emptyList())

    override val oneShot: CoroutineLauncher = object : CoroutineLauncher {
        override val scope: CoroutineScope get() = source.scope
        override val loading: MutableStateFlow<Int> get() = source.loading

        override fun rawLaunch(
            context: CoroutineContext,
            start: CoroutineStart,
            block: suspend CoroutineScope.() -> Unit,
        ): Job {
            oneShotJobs.increment()
            return super.rawLaunch(context, start, block).apply {
                invokeOnCompletion {
                    it?.let { errors.replace { plus(it) } }
                    oneShotJobs.decrement()
                }
            }
        }

        override fun onError(error: Throwable) {
            errors.replace { plus(error) }
        }
    }

    suspend fun process() {
        runCatching { observer() }.exceptionOrNull()?.let { errors.replace { plus(it) } }
        oneShotJobs.filter { it == 0 }.first()
    }
}
