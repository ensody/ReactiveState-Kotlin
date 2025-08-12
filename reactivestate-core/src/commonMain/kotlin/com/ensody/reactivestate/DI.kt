package com.ensody.reactivestate

import com.ensody.reactivestate.derived
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KClass
import com.ensody.reactivestate.derived as realDerived

/**
 * The default dependency injection graph.
 *
 * This supplements purely code-based DI solutions (which use `by lazy` instead of `@Singleton` etc.) by
 * tracking the interdependencies between the DI nodes and re-creating sub-graphs whenever a DI object is replaced
 * dynamically (e.g. because the app wants to reconfigure itself).
 *
 * Example how to define a code-based DI graph:
 *
 * ```kotlin
 * // -------------
 * // module foo
 * // -------------
 *
 * // Convenience accessor for FooDeps
 * val DIResolver.fooDeps: LazyProperty<FooDeps> get() = DI.run { get() }
 *
 * // The actual FooDeps DI module/node. This is a circular dependency with BarDeps below.
 * class FooDeps(
 *     val configFlag: Boolean,
 *     lazyBarDeps: LazyProperty<BarDeps>,
 * ) {
 *     // All deps have to be resolved lazily
 *     val barDeps by lazyBarDeps
 *
 *     // All deps have to be resolved lazily
 *     val circularConfigFlag by lazy { barDeps.configFlag }
 *
 *     public fun myUseCase() = MyUseCase(circularConfigFlag)
 * }
 *
 * class MyUseCase(val circularConfigFlag: Boolean)
 *
 * // -------------
 * // module bar
 * // -------------
 *
 * // Convenience accessor for BarDeps
 * val DIResolver.barDeps: LazyProperty<BarDeps> get() = DI.run { get() }
 *
 * // Circular dependency to Foo, so we have
 * class BarDeps(lazyFooDeps: LazyProperty<FooDeps>) {
 *     val fooDeps: FooDeps by lazyFooDeps
 *
 *     val configFlag: Boolean by lazy { fooDeps.configFlag }
 * }
 * ```
 */
@ExperimentalReactiveStateApi
public val DI: DIImpl = DIImpl()

/**
 * The default implementation of the dependency injection graph.
 */
@OptIn(InternalReactiveStateApi::class)
@ExperimentalReactiveStateApi
public class DIImpl {
    private val deps = mutableMapOf<KClass<*>, State>()

    public inline fun <reified T : Any> register(noinline factory: DIResolver.() -> T) {
        register(T::class, factory)
    }

    public fun <T : Any> register(klass: KClass<T>, factory: DIResolver.() -> T) {
        var lastVersion = -1
        val newValue = derived(klass) {
            val state = deps.getValue(klass)
            // If the version is changed this means a dependency has told us to refresh.
            // If it's unchanged this means a StateFlow that we depend on has changed, so now we have to tell all our
            // dependents (the factories that depend on this klass) to update themselves.
            val nextVersion = get(state.versionOfDependencies)
            if (lastVersion == nextVersion) {
                val collected = mutableSetOf<KClass<*>>()
                val toCollect = mutableSetOf<KClass<*>>(klass)
                while (toCollect.isNotEmpty()) {
                    val next = toCollect.first().also { toCollect.remove(it) }
                    if (collected.add(next)) {
                        for ((candidate, candidateState) in deps) {
                            if (candidate !in collected && collected.any { it in candidateState.dependsOn }) {
                                toCollect.add(candidate)
                            }
                        }
                    }
                }
                for (changed in collected - klass) {
                    deps.getValue(changed).versionOfDependencies.getAndUpdate {
                        // Prevent reaching -1 (the initial value) via overflow
                        (it + 1) % 1000000
                    }
                }
            } else {
                lastVersion = nextVersion
            }
            get(state.factory)()
        }
        deps[klass]?.also {
            it.versionOfDependencies.value = 0
            it.factory.value = factory
        } ?: run { deps[klass] = State(newValue, MutableStateFlow(factory)) }
    }

    public fun <T> derived(factory: DIResolver.() -> T): StateFlow<T> =
        derived(null, factory)

    private fun <T> derived(klass: KClass<*>?, factory: DIResolver.() -> T): StateFlow<T> {
        val mutex = Mutex()
        var scope: CoroutineScope? = null
        var lastResolver: DIResolverImpl? = null
        return realDerived {
            val nextScope = MainScope()
            mutex.withSpinLock {
                scope?.cancel()
                scope = nextScope
            }
            val resolver = DIResolverImpl(this, nextScope, this@DIImpl, klass).also {
                lastResolver?.scope?.cancel()
                lastResolver = it
            }
            resolver.factory().also { resolver.ready = true }
        }
    }

    // We hide this function as an extension, so nobody can mistakenly get() arbitrary T values not belonging to the DI
    public inline fun <reified T : Any> DIResolver.get(noinline default: (() -> T)? = null): LazyProperty<T> =
        InternalDI.run { get(this@get, T::class, default) }

    public fun <T : Any> InternalDI.get(
        resolver: DIResolver,
        klass: KClass<T>,
        default: (() -> T)? = null,
    ): LazyProperty<T> {
        if (default != null && klass !in deps) {
            register(klass) { default() }
        }
        val state = deps.getValue(klass)
        resolver.run { owner }?.also {
            // A DI module/node must not resolve any deps eagerly, so we can support circular dependencies.
            // This means we track a separate dependency graph and use the version to trigger updates.
            resolver.get(state.factory)
            resolver.get(state.versionOfDependencies)
            deps.getValue(it).dependsOn.add(klass)
        } ?: run {
            // A normal DI.derived call is allowed to eagerly resolve since there can be no circularity
            resolver.get(state.derived)
        }
        @Suppress("UNCHECKED_CAST")
        return lazyProperty {
            check(resolver.run { owner == null || ready }) {
                "You MUST NOT access the value before the factory function has finished. " +
                    "Otherwise circular deps are impossible."
            }
            state.derived.value as T
        }
    }

    private class State(
        val derived: StateFlow<*>,
        val factory: MutableStateFlow<DIResolver.() -> Any>,
        // This version is incremented when a dependency has changed
        val versionOfDependencies: MutableStateFlow<Int> = MutableStateFlow(0),
        val dependsOn: MutableSet<KClass<*>> = mutableSetOf(),
    )
}

@InternalReactiveStateApi
public object InternalDI

/**
 * A special [Resolver] which allows looking up dependencies from a [DI] graph.
 *
 * This interface only exists to prevent [derived] lambdas from having access to the [DI] graph.
 */
@ExperimentalReactiveStateApi
public interface DIResolver : Resolver {
    public val DI: DIImpl

    public val scope: CoroutineScope

    @InternalReactiveStateApi
    public val InternalDI.owner: KClass<*>?

    @InternalReactiveStateApi
    public val InternalDI.ready: Boolean
}

@ExperimentalReactiveStateApi
public fun <T> DIResolver.get(lazyProperty: LazyProperty<T>): T =
    lazyProperty.getValue(null, ::DI)

private class DIResolverImpl(
    delegate: Resolver,
    override val scope: CoroutineScope,
    override val DI: DIImpl,
    private val klass: KClass<*>?,
    var ready: Boolean = false,
) : DIResolver, Resolver by delegate {
    @InternalReactiveStateApi
    override val InternalDI.owner: KClass<*>? get() = klass

    @InternalReactiveStateApi
    override val InternalDI.ready: Boolean
        get() = this@DIResolverImpl.ready
}
