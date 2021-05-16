package com.ensody.reactivestate.android

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.ensody.reactivestate.*
import com.ensody.reactivestate.android.*
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

/**
 * Creates a multiplatform [ReactiveState] ViewModel and observes its [ReactiveState.eventNotifier].
 *
 * The [provider] should instantiate the object directly.
 */
public inline fun <reified E : ErrorEvents, reified T : ReactiveState<E>> Fragment.reactiveState(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> {
    if (this !is E) {
        throw IllegalStateException("Your Fragment has to implement the ViewModel's events interface.")
    }
    val result = buildOnViewModel(provider)
    lifecycleScope.launchWhenCreated {
        result.value.eventNotifier.handleEvents(this@reactiveState as E, this@reactiveState)
    }
    return result
}

/**
 * Creates a multiplatform [ReactiveState] ViewModel and observes its [ReactiveState.eventNotifier].
 *
 * The [provider] should instantiate the object directly.
 */
public inline fun <reified E : ErrorEvents, reified T : ReactiveState<E>> ComponentActivity.reactiveState(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> {
    if (this !is E) {
        throw IllegalStateException("Your Activity has to implement the ViewModel's events interface.")
    }
    val result = buildOnViewModel(provider)
    lifecycleScope.launchWhenCreated {
        result.value.eventNotifier.handleEvents(this@reactiveState as E, this@reactiveState)
    }
    return result
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
    stateFlowViewModel { WrapperViewModel(it) }.buildOnViewModel(provider)

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
    stateFlowViewModel { WrapperViewModel(it) }.buildOnViewModel(provider)

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
public inline fun <reified T : Any> Lazy<WrapperViewModel>.buildOnViewModel(
    crossinline provider: BuildOnViewModelContext.() -> T,
): Lazy<T> = lazy {
    val stateFlowStore = NamespacedStateFlowStore(
        store = value.stateFlowStore,
        namespace = T::class.qualifiedName
            ?: throw IllegalArgumentException("The class must have a qualifiedName"),
    )
    val result = value.registry[T::class] as? T
        ?: BuildOnViewModelContext(scope = value.viewModelScope, stateFlowStore = stateFlowStore).provider()
    value.registry[T::class] = result
    result
}
