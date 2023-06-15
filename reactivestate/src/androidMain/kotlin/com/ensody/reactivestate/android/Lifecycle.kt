package com.ensody.reactivestate.android

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ensody.reactivestate.Disposable
import com.ensody.reactivestate.OnDemandStateFlow
import com.ensody.reactivestate.OnDispose
import com.ensody.reactivestate.autoRun
import com.ensody.reactivestate.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private abstract class DisposableObserver(private val lifecycle: Lifecycle) :
    DefaultLifecycleObserver,
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
    override fun onStart(owner: LifecycleOwner) {
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
    override fun onStop(owner: LifecycleOwner) {
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
    override fun onCreate(owner: LifecycleOwner) {
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
    override fun onDestroy(owner: LifecycleOwner) {
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
    override fun onResume(owner: LifecycleOwner) {
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
    override fun onPause(owner: LifecycleOwner) {
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
 * A [StateFlow] that tracks the current [Lifecycle.State].
 */
public fun LifecycleStateFlow(lifecycle: Lifecycle): StateFlow<Lifecycle.State> =
    OnDemandStateFlow({ lifecycle.currentState }) {
        val observer = LifecycleEventObserver { _, event -> value = event.targetState }
        lifecycle.addObserver(observer)
        invokeOnCancellation { lifecycle.removeObserver(observer) }
    }

/** Tracks the current lifecycle state as a [StateFlow] and passes it to the given [block]. */
public suspend fun <T> LifecycleOwner.withLifecycleStateFlow(block: suspend (StateFlow<Lifecycle.State>) -> T): T =
    block(LifecycleStateFlow(lifecycle))

/**
 * Waits until the lifecycle reaches the given [state] and then launches a coroutine with the given [block].
 *
 * @param cancelWhenBelow Pass `false` to keep the coroutine running even if the state falls below [state].
 */
public fun LifecycleOwner.launchOnceStateAtLeast(
    state: Lifecycle.State,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    cancelWhenBelow: Boolean = true,
    block: suspend CoroutineScope.() -> Unit,
): Job =
    lifecycleScope.launch(context, start) {
        onceStateAtLeast(state, cancelWhenBelow = cancelWhenBelow) {
            block()
        }
    }

/**
 * Waits until the lifecycle reaches the given [state] and then runs [block].
 *
 * @param cancelWhenBelow Pass `false` to keep the coroutine running even if the state falls below [state].
 */
public suspend fun <T> LifecycleOwner.onceStateAtLeast(
    state: Lifecycle.State,
    cancelWhenBelow: Boolean,
    block: suspend () -> T,
): T {
    val currentState = LifecycleStateFlow(lifecycle)
    currentState.filter { it >= state }.first()
    return if (cancelWhenBelow) {
        coroutineScope {
            val abort = async {
                currentState.filter { it < state }.map { this@coroutineScope.cancel() }.first()
            }
            block().also { abort.cancel() }
        }
    } else {
        block()
    }
}
