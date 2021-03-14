package com.ensody.reactivestate.android

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ensody.reactivestate.ErrorEvents
import com.ensody.reactivestate.EventNotifier
import com.ensody.reactivestate.handleEvents
import kotlinx.coroutines.launch

/**
 * Consumes and handles [EventNotifier]'s events on the given [handler], but only when [owner] is in >=STARTED state.
 *
 * IMPORTANT: You have to call this function exactly once for the whole lifecycle of [owner].
 * Usually that means `Activity.onCreate`/`Fragment.onViewCreated`.
 *
 * Any errors during event handling will trigger [ErrorEvents.onError] on the [handler].
 *
 * WARNING: Try to avoid switching threads within your [handler]'s methods! Otherwise your operation can get canceled
 * and lost when rotating or locking the screen. Always stay on the main thread.
 */
public fun <T : ErrorEvents> EventNotifier<T>.handleEvents(handler: T, owner: LifecycleOwner) {
    // XXX: We don't use `launchWhenStarted` because it has its quirks (though our alternative has gotchas, too):
    //
    // If you're outside of the main thread (e.g. `withContext(dispatchers.io)`), `launchWhenStarted` fails to pause
    // the coroutine in `onStop()`! It will just continue running until the end of `withContext` and then get paused.
    // So you mustn't interact with the UI outside of the main thread. This sounds obvious, but in practice it's much
    // too easy to mistakenly write code that violates this rule.
    //
    // If you receive an event during `onStop()`, the `Flow.collect` call in `launchWhenStarted` still consumes an
    // event, but doesn't execute it, so the event is lost during screen rotation!
    //
    // Our alternative of canceling the coroutine in `onStop()` means that if your event handler is not in the main
    // thread it gets canceled when locking the screen.
    //
    // In both solutions, rotating the screen while you're not in the main thread means your operation is canceled and
    // lost. So it's best to avoid switching threads in the UI. Always stay on the main thread.
    owner.onStart {
        val job = owner.lifecycleScope.launch {
            handleEvents(handler)
        }
        owner.onStopOnce { job.cancel() }
    }
}
