package com.ensody.reactivestate.android

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ensody.reactivestate.ErrorEvents
import com.ensody.reactivestate.SimpleCoroutineLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/** A [SimpleCoroutineLauncher] that launches coroutines in the `STARTED` state. */
public class LifecycleCoroutineLauncher(
    public val owner: LifecycleOwner,
) : SimpleCoroutineLauncher(owner.lifecycleScope) {

    override fun rawLaunch(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> Unit,
    ): Job =
        owner.launchOnceStateAtLeast(Lifecycle.State.STARTED, context, start) {
            block()
        }

    override fun onError(error: Throwable) {
        if (owner is ErrorEvents) {
            owner.launchOnceStateAtLeast(Lifecycle.State.STARTED) { owner.onError(error) }
        } else {
            super.onError(error)
        }
    }
}
