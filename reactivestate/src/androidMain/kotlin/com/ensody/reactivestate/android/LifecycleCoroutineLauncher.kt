package com.ensody.reactivestate.android

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.ensody.reactivestate.ErrorEvents
import com.ensody.reactivestate.SimpleCoroutineLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/** A [SimpleCoroutineLauncher] that launches coroutines in the `STARTED` state. */
public class LifecycleCoroutineLauncher(
    public val owner: LifecycleOwner
) : SimpleCoroutineLauncher(owner.lifecycleScope) {

    override fun rawLaunch(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return owner.lifecycleScope.launch(context, start) {
            owner.whenStarted(block)
        }
    }

    override fun onError(error: Throwable) {
        if (owner is ErrorEvents) {
            owner.lifecycleScope.launchWhenStarted { owner.onError(error) }
        } else {
            super.onError(error)
        }
    }
}
