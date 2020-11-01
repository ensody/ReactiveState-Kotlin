package com.ensody.reactivestate

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

private abstract class DisposableObserver(private val lifecycle: Lifecycle) :
    LifecycleObserver,
    Disposable {

    override fun dispose() {
        lifecycle.removeObserver(this)
    }
}

private fun LifecycleOwner.addObserver(observer: DisposableObserver): Disposable {
    lifecycle.addObserver(observer)
    return observer
}

private fun Fragment.addViewLifecycleObserver(
    once: Boolean = false,
    create: (LifecycleOwner) -> Disposable
): Disposable {
    val group = DisposableGroup()
    val onDestroyDisposable = DisposableGroup()
    group.add(onDestroyDisposable)
    // We have to use lifecycleScope.autoRun because LifecycleOwner.autoRun only runs within one
    // single onCreateView/onDestroyView cycle. Here we want to execute autoRun during the whole lifetime.
    group.add(
        lifecycleScope.autoRun {
            onDestroyDisposable.dispose()
            get(viewLifecycleOwnerLiveData)?.let {
                onDestroyDisposable.add(create(it))
                if (once) {
                    autoRunner.dispose()
                    group.dispose()
                }
            }
        }
    )
    return group
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
 * Runs the given block on every `Lifecycle.Event.ON_START`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onStart(block: () -> Unit): Disposable =
    addObserver(OnStartObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next `Lifecycle.Event.ON_START`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onStartOnce(block: () -> Unit): Disposable =
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
 * Runs the given block on every `Lifecycle.Event.ON_STOP`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onStop(block: () -> Unit): Disposable =
    addObserver(OnStopObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next `Lifecycle.Event.ON_STOP`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onStopOnce(block: () -> Unit): Disposable =
    addObserver(OnStopObserver(lifecycle, true, block))

private class OnCreateObserver(
    lifecycle: Lifecycle,
    private val once: Boolean,
    private val block: () -> Unit
) : DisposableObserver(lifecycle) {
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun handle() {
        block()
        if (once) {
            dispose()
        }
    }
}

/**
 * Runs the given block on every `Lifecycle.Event.ON_CREATE`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onCreate(block: () -> Unit): Disposable =
    addObserver(OnCreateObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next `Lifecycle.Event.ON_DESTROY`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onCreateOnce(block: () -> Unit): Disposable =
    addObserver(OnCreateObserver(lifecycle, true, block))

private class OnDestroyObserver(
    lifecycle: Lifecycle,
    private val once: Boolean,
    private val block: () -> Unit
) : DisposableObserver(lifecycle) {
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun handle() {
        block()
        if (once) {
            dispose()
        }
    }
}

/**
 * Runs the given block on every `Lifecycle.Event.ON_DESTROY`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onDestroy(block: () -> Unit): Disposable =
    addObserver(OnDestroyObserver(lifecycle, false, block))

/**
 * Runs the given block on every `Fragment.onCreateView` (actually `onViewStateRestored`).
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun Fragment.onCreateView(block: () -> Unit): Disposable =
    addViewLifecycleObserver { it.onCreate(block) }

/**
 * Runs the given block once on the next `Fragment.onCreateView` (actually `onViewStateRestored`).
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun Fragment.onCreateViewOnce(block: () -> Unit): Disposable =
    addViewLifecycleObserver(once = true) { it.onCreateOnce(block) }

/**
 * Runs the given block once on the next `Lifecycle.Event.ON_DESTROY`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onDestroyOnce(block: () -> Unit): Disposable =
    addObserver(OnDestroyObserver(lifecycle, true, block))

/**
 * Runs the given block on every `Fragment.onDestroyView`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun Fragment.onDestroyView(block: () -> Unit): Disposable =
    addViewLifecycleObserver { it.onDestroy(block) }

/**
 * Runs the given block once on the next `Fragment.onDestroyView`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun Fragment.onDestroyViewOnce(block: () -> Unit): Disposable =
    addViewLifecycleObserver(once = true) { it.onDestroyOnce(block) }

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
 * Runs the given block on every `Lifecycle.Event.ON_RESUME`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onResume(block: () -> Unit): Disposable =
    addObserver(OnResumeObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next `Lifecycle.Event.ON_RESUME`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onResumeOnce(block: () -> Unit): Disposable =
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
 * Runs the given block on every `Lifecycle.Event.ON_PAUSE`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onPause(block: () -> Unit): Disposable =
    addObserver(OnPauseObserver(lifecycle, false, block))

/**
 * Runs the given block once on the next `Lifecycle.Event.ON_PAUSE`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun LifecycleOwner.onPauseOnce(block: () -> Unit): Disposable =
    addObserver(OnPauseObserver(lifecycle, true, block))

/**
 * Launches and runs the given block when and as long as the `Lifecycle` controlling this
 * `LifecycleCoroutineScope` is at least in the `Lifecycle.State.STARTED` state.
 *
 * This is useful for e.g. processing a `Flow` or `Channel` of events only while the Fragment is
 * started.
 *
 * The returned `Job` will be canceled when the `Lifecycle` is stopped (`Lifecycle.Event.ON_STOP`).
 * This means you should usually call this function in e.g. `Fragment.onStart()`.
 */
public fun LifecycleOwner.launchWhileStarted(block: suspend CoroutineScope.() -> Unit): Job {
    val job = lifecycleScope.launchWhenStarted {
        block()
    }
    onStopOnce { job.cancel() }.disposeOnCompletionOf(job)
    return job
}

/**
 * Launches and runs the given block when and as long as the `Lifecycle` controlling this
 * `LifecycleCoroutineScope` is at least in the `Lifecycle.State.RESUMED` state.
 *
 * This is useful for e.g. processing a `Flow` or `Channel` of events only while the Fragment is
 * resumed.
 *
 * The returned `Job` will be canceled when the `Lifecycle` is paused (`Lifecycle.Event.ON_PAUSE`).
 * This means you should usually call this function in e.g. `Fragment.onResume()`.
 */
public fun LifecycleOwner.launchWhileResumed(block: suspend CoroutineScope.() -> Unit): Job {
    val job = lifecycleScope.launchWhenResumed {
        block()
    }
    onPauseOnce { job.cancel() }.disposeOnCompletionOf(job)
    return job
}
