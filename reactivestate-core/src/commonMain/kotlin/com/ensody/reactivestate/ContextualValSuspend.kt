package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * A `val` for which the value can be set via the [coroutineContext].
 *
 * This is similar to a thread-local or the "dynamic scope" concept.
 *
 * The [default] value is created lazily on demand per [CoroutineScope] (not globally!).
 * For this to work you have to inject [ContextualValRoot] into each [CoroutineScope] that wants to support
 * this class.
 *
 * The [name] is only used to help with debugging.
 *
 * Also see [ContextualVal] for a blocking version (not suspend).
 */
public class ContextualValSuspend<T>(
    public val name: String,
    public var default: suspend (CoroutineContext) -> T,
) {
    public val key: ContextKey<T> = ContextKey(this)
    private val globalDefaultCache = LazySuspend { default(EmptyCoroutineContext) }

    /** Gets the value for the current [coroutineContext]. */
    public suspend fun get(): T =
        get(coroutineContext)

    /** Gets the value for the given [scope]. */
    public suspend fun get(scope: CoroutineScope): T =
        get(scope.coroutineContext)

    /** Gets the value for the given [context]. */
    @Suppress("UNCHECKED_CAST")
    public suspend fun get(context: CoroutineContext): T {
        context[key]?.let { return it.get(context) }
        context[ContextualValRootInternal.key]?.let {
            return it.get(context).getOrPut(this) { default(context) } as T
        }
        return globalDefaultCache.get()
    }

    /** Sets a new [value] that only exists within [block]. */
    public suspend fun <R> with(value: T, block: suspend () -> R): R =
        withContext(valued { value }) { block() }

    /** The returned [CoroutineContext.Element] can used to set a value via [CoroutineScope.plus]. */
    public fun valued(block: suspend (CoroutineContext) -> T): CoroutineContext.Element =
        ContextElement(key, block)

    override fun toString(): String =
        "${super.toString()}<$name>"

    public class ContextKey<T>(public val value: ContextualValSuspend<T>) : CoroutineContext.Key<ContextElement<T>> {
        override fun toString(): String =
            "${super.toString()}<$value>"
    }

    public class ContextElement<T>(
        override val key: ContextKey<T>,
        private val valueGetter: suspend (CoroutineContext) -> T,
    ) : CoroutineContext.Element {
        private val value: MutableStateFlow<Wrapped<T>?> = MutableStateFlow(null)

        internal tailrec suspend fun get(context: CoroutineContext): T {
            value.value?.let { return it.value }
            value.compareAndSet(null, Wrapped(valueGetter(context)))
            return get(context)
        }

        override fun toString(): String =
            "${super.toString()}<${key.value}>"
    }
}
