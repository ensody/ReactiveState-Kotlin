package com.ensody.reactivestate

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * An object that can be disposed/deactivated/canceled by calling `dispose()`.
 *
 * This is an alias for [DisposableHandle].
 */
typealias Disposable = DisposableHandle

/**
 * A [Disposable] that can have additional `Disposable`s attached to it, so they are automatically
 * disposed together with this object.
 */
interface AttachedDisposables : Disposable {
    /** The attached disposables which should be auto-disposed when this object is disposed. */
    val attachedDisposables: DisposableGroup
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
class OnDispose(val function: () -> Unit) : Disposable {
    override fun dispose() {
        function()
    }
}

/** A [Disposable] wrapping a [Job]. */
class JobDisposable(val job: Job) : Disposable {
    override fun dispose() {
        job.cancel()
    }
}

/**
 * A [Disposable] that can dispose multiple [Disposable] and [Job] instances
 * at once.
 */
class DisposableGroup : Disposable {
    private val disposables = mutableSetOf<Disposable>()
    private val jobs = mutableSetOf<Job>()

    /** Add a [Disposable] to this group. */
    fun add(disposable: Disposable) {
        if (disposable is JobDisposable) {
            add(disposable.job)
        } else {
            disposables.add(disposable)
        }
    }

    /** Add a [Job] to this group. */
    fun add(job: Job) {
        jobs.add(job)
    }

    /** Remove a [Disposable] from this group. */
    fun remove(disposable: Disposable) {
        if (disposable is JobDisposable) {
            remove(disposable.job)
        } else {
            disposables.remove(disposable)
        }
    }

    /** Remove a [Job] from this group. */
    fun remove(job: Job) {
        jobs.remove(job)
    }

    /** Disposes all [Disposable] and [Job] instances in this group. */
    override fun dispose() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        disposables.forEach { it.dispose() }
        disposables.clear()
    }
}

/** Helper for adding a completion handler to a [CoroutineContext]. */
internal fun CoroutineContext.invokeOnCompletion(handler: CompletionHandler): Disposable =
    this[Job]!!.invokeOnCompletion(handler)

/** Helper for adding a completion handler to a [CoroutineScope]. */
internal fun CoroutineScope.invokeOnCompletion(handler: CompletionHandler): Disposable =
    coroutineContext.invokeOnCompletion(handler)

/** Disposes the [Disposable] when [Job] completes (including cancellation). */
fun Disposable.disposeOnCompletionOf(job: Job): Disposable =
    job.invokeOnCompletion { dispose() }

/** Disposes the [Disposable] when [CoroutineContext] completes (including cancellation). */
fun Disposable.disposeOnCompletionOf(context: CoroutineContext): Disposable =
    context.invokeOnCompletion { dispose() }

/** Disposes the [Disposable] when [CoroutineScope] completes (including cancellation). */
fun Disposable.disposeOnCompletionOf(scope: CoroutineScope): Disposable =
    scope.invokeOnCompletion { dispose() }
