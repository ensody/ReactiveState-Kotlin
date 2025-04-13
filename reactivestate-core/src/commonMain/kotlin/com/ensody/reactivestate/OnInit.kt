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
import kotlin.js.JsName

/**
 * A mechanism for ViewModel initialization-time tasks like repository cache refresh. Use via [ContextualOnInit].
 *
 * With [observe] you get notified when the ViewModel is ready. On the provided [OnInitContext] you can launch
 * coroutines which are associated with your observer. If
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
    public fun trigger(source: CoroutineLauncher)

    public sealed interface State {
        public data object Initializing : State
        public data class Error(val errors: List<Throwable>) : State
        public data object Finished : State
    }
}

/**
 * Provides access to a ViewModel's [OnInit] instance.
 */
@ExperimentalReactiveStateApi
public val ContextualOnInit: ContextualVal<OnInit> = ContextualVal("ContextualOnInit") { OnInit() }

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

    /** Launcher for tasks that will never terminate (e.g. you're collecting a [StateFlow] until the ViewModel dies). */
    public val permanent: CoroutineLauncher

    /** The underlying ViewModel in case you might need its [CoroutineLauncher.scope] for [ContextualVal] resolution. */
    public val source: CoroutineLauncher
}

@JsName("createOnInit")
@ExperimentalReactiveStateApi
public fun OnInit(): OnInit =
    OnInitImpl()

private class OnInitImpl : OnInit {
    private var source: CoroutineLauncher? = null
    private val observers: MutableStateFlow<List<OnInitContext.() -> Unit>> = MutableStateFlow(emptyList())
    private val mutex = Mutex()

    override val state: MutableStateFlow<OnInit.State> = MutableStateFlow(OnInit.State.Initializing)

    override fun observe(block: OnInitContext.() -> Unit) {
        observers.replace { plus(block) }
        source?.let {
            trigger(it)
        }
    }

    override fun unobserve(block: OnInitContext.() -> Unit) {
        observers.replace { minus(block) }
    }

    override fun trigger(source: CoroutineLauncher) {
        if (this.source == null) {
            this.source = source
        }

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
                                    unobserve(context.observer)
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
}

private class OnInitContextImpl(
    override val source: CoroutineLauncher,
    val observer: OnInitContext.() -> Unit,
) : OnInitContext {
    val oneShotJobs: MutableStateFlow<Int> = MutableStateFlow(0)
    val errors: MutableStateFlow<List<Throwable>> = MutableStateFlow(emptyList())

    override val oneShot: CoroutineLauncher = object : CoroutineLauncher by source {
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
    }

    override val permanent: CoroutineLauncher = source

    suspend fun process() {
        runCatching { observer() }.exceptionOrNull()?.let { errors.replace { plus(it) } }
        oneShotJobs.filter { it == 0 }.first()
    }
}
