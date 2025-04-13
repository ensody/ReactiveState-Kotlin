package com.ensody.reactivestate.android

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.ensody.reactivestate.ContextualErrorsFlow
import com.ensody.reactivestate.ContextualStateFlowStore
import com.ensody.reactivestate.ContextualValRoot
import com.ensody.reactivestate.CoroutineLauncher
import com.ensody.reactivestate.ErrorEvents
import com.ensody.reactivestate.ExperimentalReactiveStateApi
import com.ensody.reactivestate.NamespacedStateFlowStore
import com.ensody.reactivestate.OnReactiveStateAttached
import com.ensody.reactivestate.OnReactiveStateAttachedTo
import com.ensody.reactivestate.ReactiveState
import com.ensody.reactivestate.ReactiveViewModel
import com.ensody.reactivestate.StateFlowStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.collections.set
import kotlin.reflect.KClass

/**
 * Creates a multiplatform [ReactiveViewModel] ViewModel.
 *
 * The [provider] should instantiate the object directly.
 */
@ExperimentalReactiveStateApi
public inline fun <reified T : CoroutineLauncher> Fragment.reactiveViewModel(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> =
    buildOnViewModel(provider).apply { attachLazyReactiveViewModel(this@reactiveViewModel) }

/**
 * Creates a multiplatform [ReactiveViewModel] ViewModel.
 *
 * The [provider] should instantiate the object directly.
 */
@ExperimentalReactiveStateApi
public inline fun <reified T : CoroutineLauncher> ComponentActivity.reactiveViewModel(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> =
    buildOnViewModel(provider).apply { attachLazyReactiveViewModel(this@reactiveViewModel) }

public fun Lazy<CoroutineLauncher>.attachLazyReactiveViewModel(
    owner: LifecycleOwner,
) {
    if (owner !is ErrorEvents) {
        throw IllegalStateException("You have to implement the ErrorEvents interface.")
    }
    owner.onStart {
        val viewModel = value
        val emittedErrors = ContextualErrorsFlow.get(viewModel.scope)
        val job = owner.lifecycleScope.launch {
            emittedErrors.collect(owner::onError)
        }
        owner.onStopOnce { job.cancel() }
    }
    owner.launchOnceStateAtLeast(Lifecycle.State.CREATED) {
        (value as? OnReactiveStateAttachedTo)?.onReactiveStateAttachedTo(owner)
    }
}

/**
 * Creates a multiplatform [ReactiveState] ViewModel and observes its [ReactiveState.eventNotifier].
 *
 * The [provider] should instantiate the object directly.
 */
public inline fun <reified E : ErrorEvents, reified T : ReactiveState<E>> Fragment.reactiveState(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> =
    buildOnViewModel(provider).apply { attachLazyReactiveState(this@reactiveState as? E, this@reactiveState) }

/**
 * Creates a multiplatform [ReactiveState] ViewModel and observes its [ReactiveState.eventNotifier].
 *
 * The [provider] should instantiate the object directly.
 */
public inline fun <reified E : ErrorEvents, reified T : ReactiveState<E>> ComponentActivity.reactiveState(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> =
    buildOnViewModel(provider).apply { attachLazyReactiveState(this@reactiveState as? E, this@reactiveState) }

public fun <E : ErrorEvents> Lazy<ReactiveState<E>>.attachLazyReactiveState(
    handler: E?,
    owner: LifecycleOwner,
) {
    if (handler == null) {
        throw IllegalStateException("You have to implement the ViewModel's events interface.")
    }
    owner.launchOnceStateAtLeast(Lifecycle.State.CREATED) {
        value.eventNotifier.handleEvents(handler, owner)
    }
    owner.launchOnceStateAtLeast(Lifecycle.State.CREATED) {
        (owner as? OnReactiveStateAttached)?.onReactiveStateAttached(value)
        (value as? OnReactiveStateAttachedTo)?.onReactiveStateAttachedTo(owner)
    }
}

/**
 * Creates an object living on a wrapper [ViewModel]. This allows for building multiplatform ViewModels.
 *
 * The [provider] should instantiate the object directly.
 *
 * @see [reactiveState] if you want to instantiate a multiplatform [ReactiveState] ViewModel directly.
 */
public inline fun <reified T : Any> Fragment.buildOnViewModel(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> =
    stateFlowViewModel { WrapperViewModel(it) }.buildOnViewModel(T::class, { it as? T }) { provider() }

/**
 * Creates an object living on a wrapper `ViewModel`. This allows for building multiplatform ViewModels.
 *
 * The [provider] should instantiate the object directly.
 *
 * @see [reactiveState] if you want to instantiate a multiplatform [ReactiveState] ViewModel directly.
 */
public inline fun <reified T : Any> ComponentActivity.buildOnViewModel(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> =
    stateFlowViewModel { WrapperViewModel(it) }.buildOnViewModel(T::class, { it as? T }) { provider() }

/** Build context for [buildOnViewModel]. */
public class BuildOnViewModelContext(
    /** The [viewModelScope]. */
    public val scope: CoroutineScope,

    /** A [StateFlowStore] where you can store/load the saved instance state (similar to a [SavedStateHandle]). */
    public val stateFlowStore: StateFlowStore,
)

/** The wrapper ViewModel used by [buildOnViewModel]. */
public class WrapperViewModel(public val stateFlowStore: StateFlowStore) : ViewModel() {
    public val registry: MutableMap<KClass<*>, Any> = mutableMapOf()
}

/** Used internally by [buildOnViewModel]. */
public fun <T : Any> Lazy<WrapperViewModel>.buildOnViewModel(
    klass: KClass<T>,
    caster: (Any?) -> T?,
    provider: BuildOnViewModelContext.() -> T,
): Lazy<T> = lazy {
    val stateFlowStore = NamespacedStateFlowStore(
        store = value.stateFlowStore,
        namespace = klass.qualifiedName
            ?: throw IllegalArgumentException("The class must have a qualifiedName"),
    )
    val result = caster(value.registry[klass])
        ?: BuildOnViewModelContext(
            scope = value.viewModelScope + ContextualValRoot() + ContextualStateFlowStore.valued { stateFlowStore },
            stateFlowStore = stateFlowStore,
        ).provider()
    value.registry[klass] = result
    result
}
