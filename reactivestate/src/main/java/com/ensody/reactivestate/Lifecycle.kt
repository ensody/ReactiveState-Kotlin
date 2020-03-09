package com.ensody.reactivestate

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/** Interface for an object that can be disposed/deactivated. */
interface Disposable {
    fun dispose()
}

/** A [Disposable] wrapping a [Job]. */
class JobDisposable(val job: Job) : Disposable {
    override fun dispose() {
        job.cancel()
    }
}

/** A [Disposable] that can dispose multiple [Disposable] or [Job] instances at once. */
open class DisposableGroup : Disposable {
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

private abstract class DisposableObserver(private val lifecycle: Lifecycle) : LifecycleObserver,
    Disposable {

    override fun dispose() {
        lifecycle.removeObserver(this)
    }
}

private fun LifecycleOwner.addObserver(observer: DisposableObserver): Disposable {
    lifecycle.addObserver(observer)
    return observer
}

private class OnStartObserver(
    lifecycle: Lifecycle,
    private val once: Boolean,
    private val block: () -> Unit
) : DisposableObserver(lifecycle) {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun handle() {
        block()
        if (once) {
            dispose()
        }
    }
}

/**
 * Runs the given block on every [Lifecycle.Event.ON_START].
 *
 * @return [Disposable] that allows removing the observer.
 * */
fun LifecycleOwner.onStart(block: () -> Unit): Disposable =
    addObserver(OnStartObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next [Lifecycle.Event.ON_START].
 *
 * @return [Disposable] that allows removing the observer.
 * */
fun LifecycleOwner.onStartOnce(block: () -> Unit): Disposable =
    addObserver(OnStartObserver(lifecycle, true, block))

private class OnStopObserver(
    lifecycle: Lifecycle,
    private val once: Boolean,
    private val block: () -> Unit
) : DisposableObserver(lifecycle) {
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun handle() {
        block()
        if (once) {
            dispose()
        }
    }
}

/**
 * Runs the given block on every [Lifecycle.Event.ON_STOP].
 *
 * @return [Disposable] that allows removing the observer.
 * */
fun LifecycleOwner.onStop(block: () -> Unit): Disposable =
    addObserver(OnStopObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next [Lifecycle.Event.ON_STOP].
 *
 * @return [Disposable] that allows removing the observer.
 * */
fun LifecycleOwner.onStopOnce(block: () -> Unit): Disposable =
    addObserver(OnStopObserver(lifecycle, true, block))

/**
 * Launches and runs the given block when and as long as the [Lifecycle] controlling this
 * [LifecycleCoroutineScope] is at least in the [Lifecycle.State.STARTED] state.
 *
 * The returned [Job] will be canceled when the [Lifecycle] is stopped ([Lifecycle.Event.ON_STOP]).
 */
fun LifecycleOwner.launchWhileStarted(block: suspend CoroutineScope.() -> Unit): Job {
    val job = lifecycleScope.launchWhenStarted {
        block()
    }
    onStopOnce { job.cancel() }
    return job
}
