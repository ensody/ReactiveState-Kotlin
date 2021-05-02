package com.ensody.reactivestate

import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * An object that can be disposed/deactivated/canceled by calling `dispose()`.
 *
 * This is an alias for [DisposableHandle].
 */
public typealias Disposable = DisposableHandle

/**
 * A [Disposable] that can have additional `Disposable`s attached to it, so they are automatically
 * disposed together with this object.
 */
public interface AttachedDisposables : Disposable {
    /** The attached disposables which should be auto-disposed when this object is disposed. */
    public val attachedDisposables: DisposableGroup

    /** Default implementation disposing the [attachedDisposables]. */
    override fun dispose() {
        attachedDisposables.dispose()
    }
}

/**
 * A [Disposable] executing the given [function] on `dispose()`.
 *
 * Example:
 *
 * ```kotlin
 * val disposable = OnDispose { println("disposing myself") }
 * disposable.dispose() // => "disposing myself"
 * ```
 */
public class OnDispose(private val function: () -> Unit) : Disposable {
    override fun dispose() {
        function()
    }
}

/** A [Disposable] wrapping a [Job]. */
public class JobDisposable(internal val job: Job) : Disposable {
    override fun dispose() {
        job.cancel()
    }
}

/**
 * A [Disposable] that can dispose multiple [Disposable] and [Job] instances at once.
 *
 * On [dispose] this destroys all [Disposable] and [Job] instances attached to it.
 */
public interface DisposableGroup : Disposable {
    public val size: Int

    /** Add a [Disposable] to this group. */
    public fun add(disposable: Disposable)

    /** Add a [Job] to this group. */
    public fun add(job: Job)

    /** Remove a [Disposable] from this group. */
    public fun remove(disposable: Disposable)

    /** Remove a [Job] from this group. */
    public fun remove(job: Job)
}

/** Constructs a [DisposableGroup]. */
public fun DisposableGroup(): DisposableGroup = DisposableGroupImpl()

private class DisposableGroupImpl : DisposableGroup {
    private val disposables = mutableSetOf<Disposable>()
    private val jobs = mutableSetOf<Job>()

    override val size: Int get() = disposables.size + jobs.size

    override fun add(disposable: Disposable) {
        if (disposable is JobDisposable) {
            add(disposable.job)
        } else {
            disposables.add(disposable)
        }
    }

    override fun add(job: Job) {
        jobs.add(job)
    }

    override fun remove(disposable: Disposable) {
        if (disposable is JobDisposable) {
            remove(disposable.job)
        } else {
            disposables.remove(disposable)
        }
    }

    override fun remove(job: Job) {
        jobs.remove(job)
    }

    override fun dispose() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        disposables.forEach { it.dispose() }
        disposables.clear()
    }
}

/** Helper for adding a completion handler to a [CoroutineContext]. */
public fun CoroutineContext.invokeOnCompletion(handler: CompletionHandler): Disposable =
    this[Job]!!.invokeOnCompletion(handler)

/** Helper for adding a completion handler to a [CoroutineScope]. */
public fun CoroutineScope.invokeOnCompletion(handler: CompletionHandler): Disposable =
    coroutineContext.invokeOnCompletion(handler)

/** Helper for adding a completion handler to a [CoroutineLauncher]. */
public fun CoroutineLauncher.invokeOnCompletion(handler: CompletionHandler): Disposable =
    launcherScope.invokeOnCompletion(handler)

/** Disposes the [Disposable] when [Job] completes (including cancellation). */
public fun Disposable.disposeOnCompletionOf(job: Job): Disposable =
    job.invokeOnCompletion { dispose() }

/** Disposes the [Disposable] when [CoroutineContext] completes (including cancellation). */
public fun Disposable.disposeOnCompletionOf(context: CoroutineContext): Disposable =
    context.invokeOnCompletion { dispose() }

/** Disposes the [Disposable] when [CoroutineScope] completes (including cancellation). */
public fun Disposable.disposeOnCompletionOf(scope: CoroutineScope): Disposable =
    scope.invokeOnCompletion { dispose() }

/** Disposes the [Disposable] when [CoroutineLauncher] completes (including cancellation). */
public fun Disposable.disposeOnCompletionOf(launcher: CoroutineLauncher): Disposable =
    launcher.invokeOnCompletion { dispose() }

/**
 * Creates an automatically invalidated property.
 *
 * The property starts out invalid and must be set to become valid.
 * When it becomes invalidated you have to set it, again, to make it valid.
 *
 * The property is invalidated when the provided [invalidateOn] function calls the lambda function
 * passed as its first argument.
 *
 * Example:
 *
 * ```kotlin
 * class SomeClass {
 *     var value by validUntil<String> { invalidate ->
 *         onSomeEvent { invalidate() }
 *     }
 * }
 * ```
 *
 * Android-specific example:
 *
 * ```kotlin
 * class MainFragment : Fragment() {
 *     private var binding by validUntil<MainFragmentBinding>(::onDestroyView)
 *
 *     override fun onCreateView(
 *         inflater: LayoutInflater, container: ViewGroup?,
 *         savedInstanceState: Bundle?
 *     ): View {
 *         binding = MainFragmentBinding.inflate(inflater, container, false)
 *         val username = binding.username
 *         // ...
 *         return binding.root
 *     }
 * }
 * ```
 */
public fun <T> validUntil(invalidateOn: (invalidate: () -> Unit) -> Any?): ReadWriteProperty<Any?, T> =
    DisposableProperty(invalidateOn)

private class DisposableProperty<T>(invalidateOn: (invalidate: () -> Unit) -> Any?) :
    ReadWriteProperty<Any?, T> {
    private var value: T? = null
    private var hasValue = false

    init {
        invalidateOn {
            value = null
            hasValue = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!hasValue) {
            throw IllegalStateException("The property is not set. Maybe it was disposed?")
        }
        return value as T
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        hasValue = true
    }
}
