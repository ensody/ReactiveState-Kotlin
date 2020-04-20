package com.ensody.reactivestate

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

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

private class OnResumeObserver(
    lifecycle: Lifecycle,
    private val once: Boolean,
    private val block: () -> Unit
) : DisposableObserver(lifecycle) {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun handle() {
        block()
        if (once) {
            dispose()
        }
    }
}

/**
 * Runs the given block on every [Lifecycle.Event.ON_RESUME].
 *
 * @return [Disposable] that allows removing the observer.
 * */
fun LifecycleOwner.onResume(block: () -> Unit): Disposable =
    addObserver(OnResumeObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next [Lifecycle.Event.ON_RESUME].
 *
 * @return [Disposable] that allows removing the observer.
 * */
fun LifecycleOwner.onResumeOnce(block: () -> Unit): Disposable =
    addObserver(OnResumeObserver(lifecycle, true, block))

private class OnPauseObserver(
    lifecycle: Lifecycle,
    private val once: Boolean,
    private val block: () -> Unit
) : DisposableObserver(lifecycle) {
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun handle() {
        block()
        if (once) {
            dispose()
        }
    }
}

/**
 * Runs the given block on every [Lifecycle.Event.ON_PAUSE].
 *
 * @return [Disposable] that allows removing the observer.
 * */
fun LifecycleOwner.onPause(block: () -> Unit): Disposable =
    addObserver(OnPauseObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next [Lifecycle.Event.ON_PAUSE].
 *
 * @return [Disposable] that allows removing the observer.
 * */
fun LifecycleOwner.onPauseOnce(block: () -> Unit): Disposable =
    addObserver(OnPauseObserver(lifecycle, true, block))

/**
 * Launches and runs the given block when and as long as the [Lifecycle] controlling this
 * [LifecycleCoroutineScope] is at least in the [Lifecycle.State.STARTED] state.
 *
 * This is useful for e.g. processing a `Flow` or `Channel` of events only while the Fragment is
 * started.
 *
 * The returned [Job] will be canceled when the [Lifecycle] is stopped ([Lifecycle.Event.ON_STOP]).
 * This means you should usually call this function in e.g. `Fragment.onStart()`.
 */
fun LifecycleOwner.launchWhileStarted(block: suspend CoroutineScope.() -> Unit): Job {
    val job = lifecycleScope.launchWhenStarted {
        block()
    }
    val disposable = onStopOnce { job.cancel() }
    job.invokeOnCompletion { disposable.dispose() }
    return job
}

/**
 * Launches and runs the given block when and as long as the [Lifecycle] controlling this
 * [LifecycleCoroutineScope] is at least in the [Lifecycle.State.RESUMED] state.
 *
 * This is useful for e.g. processing a `Flow` or `Channel` of events only while the Fragment is
 * resumed.
 *
 * The returned [Job] will be canceled when the [Lifecycle] is paused ([Lifecycle.Event.ON_PAUSE]).
 * This means you should usually call this function in e.g. `Fragment.onResume()`.
 */
fun LifecycleOwner.launchWhileResumed(block: suspend CoroutineScope.() -> Unit): Job {
    val job = lifecycleScope.launchWhenResumed {
        block()
    }
    val disposable = onPauseOnce { job.cancel() }
    job.invokeOnCompletion { disposable.dispose() }
    return job
}
