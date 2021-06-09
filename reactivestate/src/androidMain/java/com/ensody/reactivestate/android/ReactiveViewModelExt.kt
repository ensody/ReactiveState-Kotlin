package com.ensody.reactivestate.android

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.ensody.reactivestate.*
import kotlinx.coroutines.CoroutineScope
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.reflect.KClass

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
    owner.lifecycleScope.launchWhenCreated {
        value.eventNotifier.handleEvents(handler, owner)
    }
    owner.lifecycleScope.launchWhenCreated {
        value.attachTo(owner)
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
        ?: BuildOnViewModelContext(scope = value.viewModelScope, stateFlowStore = stateFlowStore).provider()
    value.registry[klass] = result
    result
}
