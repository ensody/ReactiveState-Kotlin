package com.ensody.reactivestate

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

/**
 * A reference-counted value that is created on-demand and freed once nobody uses it (whereas `by lazy` is never freed).
 *
 * [WhileUsed] is useful for e.g. caches or other resource-consuming values that shouldn't live forever, but only
 * exist while they're in use. Sometimes this can also be helpful for dealing with security-critical data.
 *
 * This can be a great combination with [SharingStarted.WhileSubscribed] and either [derived] or
 * [Flow.stateIn]/[Flow.shareIn], for example.
 *
 * In order to request the value with [invoke] you need a [CoroutineScope] or a [DisposableGroup].
 * Note: Your [builder] function is also passed a [DisposableGroup] for accessing other [WhileUsed] instances.
 * The [CoroutineScope]/[DisposableGroup] is used to track the requester's lifetime and in turn the reference count.
 * As an alternative when you don't have a [CoroutineScope] you can also use [disposableValue], but this is more
 * error-prone because it's easier to forget.
 *
 * Typically you'd place such values in your DI system and have one or more ViewModels or UI screens or widgets
 * requesting the value. Once these screens/widgets/ViewModels are destroyed (e.g. because the user pressed on the back
 * button) the value is freed again.
 *
 * Example:
 *
 * ```kotlin
 * val createDatabase = WhileUsed { Database() }
 * val createCache = WhileUsed { DbCache(createDatabase(it)) }
 *
 * class MyViewModel : ViewModel() {
 *     val cache = createCache(viewModelScope)
 * }
 * ```
 *
 * @param retentionMillis Defines a retention period in milliseconds in which to still keep the value in RAM
 *                        although the reference count returned to 0. If the value is requested again within
 *                        this retention period, the old value is reused. Defaults to 0 (immediately frees the value).
 * @param destructor Optional destructor which can clean up the object before it gets freed.
 *                   Defaults to `null` in which case, if the value is a [Disposable], its `dispose()` method is called.
 *                   Pass an empty lambda function if you don't want this behavior.
 * @param builder Should create and return the value. The builder gets a [DisposableGroup] as its argument for
 *                retrieving other [WhileUsed] values or for adding other [Disposable]s which must be cleaned up
 *                together with this value (as an alternative to using [destructor]).
 */
public class WhileUsed<T>(
    private val retentionMillis: Long = 0,
    private val destructor: ((T) -> Unit)? = null,
    private val builder: (WhileUsedReferenceToken) -> T,
) {
    private var value: Wrapped<T>? = null
    private var disposables = WhileUsedReferenceToken()
    private var references = 0
    private var cleaner: Job? = null

    /**
     * Creates or returns the existing value while incrementing the reference count.
     *
     * When the given [userScope] is canceled the reference count is decremented.
     * Once the count is 0 the value is freed.
     */
    public operator fun invoke(userScope: CoroutineScope): T =
        disposableValue().apply {
            disposeOnCompletionOf(userScope)
        }.value

    /**
     * Creates or returns the existing value while incrementing the reference count.
     *
     * When the given [referenceToken] is disposed the reference count is decremented.
     * Once the count is 0 the value is freed.
     */
    public operator fun invoke(referenceToken: DisposableGroup): T =
        disposableValue().also {
            referenceToken.add(it)
        }.value

    /**
     * Creates or returns the existing value while incrementing the reference count. You really want [invoke] instead.
     *
     * IMPORTANT: You have to call `dispose()` on the returned value once you stop using it.
     */
    public fun disposableValue(): DisposableValue<T> {
        synchronized(this) {
            val result = value ?: Wrapped(builder(disposables))
            cleaner?.cancel()
            cleaner = null
            value = result
            references++
            return DisposableValue(result.value, ::release)
        }
    }

    private fun release() {
        synchronized(this) {
            references--
            if (references > 0) {
                return
            }
            if (retentionMillis <= 0) {
                clear()
                return
            }
            // While usually it's not ok to use the GlobalScope we really want to make this transparent.
            cleaner = GlobalScope.launch {
                delay(retentionMillis)
                synchronized(this) {
                    if (references == 0) {
                        clear()
                    }
                }
            }
        }
    }

    private fun clear() {
        value?.also { destructor?.invoke(it.value) }
            ?: (value?.value as? Disposable)?.dispose()
        value = null
        disposables.dispose()
    }
}

/** The reference token passed to the [WhileUsed] builder function. */
public class WhileUsedReferenceToken : DisposableGroup by DisposableGroup() {
    /** A lazily created [MainScope] that lives only as long as the [WhileUsed] value. */
    public val scope: CoroutineScope
        get() =
            lazyScope ?: MainScope().also {
                lazyScope = it
                add(OnDispose {
                    lazyScope = null
                    it.cancel()
                })
            }

    private var lazyScope: CoroutineScope? = null
}

private class Wrapped<T>(val value: T)
