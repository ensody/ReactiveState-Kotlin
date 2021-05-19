package com.ensody.reactivestate.android

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.ensody.reactivestate.Disposable
import com.ensody.reactivestate.OnDispose
import com.ensody.reactivestate.autoRun
import kotlinx.coroutines.CoroutineScope

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
    scope: CoroutineScope,
    create: (LifecycleOwner) -> Disposable,
): Disposable {
    var activeObserver: Disposable? = null
    // We have to use lifecycleScope.autoRun because LifecycleOwner.autoRun only runs within one
    // single onCreateView/onDestroyView cycle. Here we want to execute autoRun during the whole lifetime.
    val autoRunner = scope.autoRun {
        get(viewLifecycleOwnerLiveData)?.let {
            activeObserver = create(it)
            if (once) {
                this.autoRunner.dispose()
            }
        }
    }
    return OnDispose {
        activeObserver?.dispose()
        autoRunner.dispose()
    }
}

private class OnStartObserver(
    lifecycle: Lifecycle,
    private val once: Boolean,
    private val block: () -> Unit,
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
    private val block: () -> Unit,
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
    private val block: () -> Unit,
) : DisposableObserver(lifecycle) {
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
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
    private val block: () -> Unit,
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
    onCreateView(lifecycleScope, block)

// For mocking in unit tests
internal fun Fragment.onCreateView(scope: CoroutineScope, block: () -> Unit): Disposable =
    addViewLifecycleObserver(scope = scope) { it.onCreate(block) }

/**
 * Runs the given block once on the next `Fragment.onCreateView` (actually `onViewStateRestored`).
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun Fragment.onCreateViewOnce(block: () -> Unit): Disposable =
    onCreateViewOnce(lifecycleScope, block)

internal fun Fragment.onCreateViewOnce(scope: CoroutineScope, block: () -> Unit): Disposable =
    addViewLifecycleObserver(once = true, scope = scope) { it.onCreateOnce(block) }

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
    onDestroyView(lifecycleScope, block)

internal fun Fragment.onDestroyView(scope: CoroutineScope, block: () -> Unit): Disposable =
    addViewLifecycleObserver(scope = scope) { it.onDestroy(block) }

/**
 * Runs the given block once on the next `Fragment.onDestroyView`.
 *
 * @return [Disposable] that allows removing the observer.
 */
public fun Fragment.onDestroyViewOnce(block: () -> Unit): Disposable =
    onDestroyViewOnce(lifecycleScope, block)

internal fun Fragment.onDestroyViewOnce(scope: CoroutineScope, block: () -> Unit): Disposable =
    addViewLifecycleObserver(once = true, scope = scope) { it.onDestroyOnce(block) }

private class OnResumeObserver(
    lifecycle: Lifecycle,
    private val once: Boolean,
    private val block: () -> Unit,
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
    private val block: () -> Unit,
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
