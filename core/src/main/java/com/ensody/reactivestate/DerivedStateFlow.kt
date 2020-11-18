package com.ensody.reactivestate

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

public fun <T> CoroutineScope.derived(started: SharingStarted, observer: AutoRunCallback<T>): StateFlow<T> {
    var onChange: () -> Unit = {}
    val autoRunner = AutoRunner(this, onChange = { onChange() }, observer = observer)
    val initialValue = CompletableDeferred<T>()
    val flow = callbackFlow {
        onChange = {
            sendBlocking(autoRunner.run())
        }
        if (started === SharingStarted.Eagerly) {
            send(initialValue.await())
        } else {
            send(autoRunner.run())
        }
        awaitClose { autoRunner.dispose() }
    }
    val realInitialValue = autoRunner.run(track = started === SharingStarted.Eagerly)
    initialValue.complete(realInitialValue)
    return flow.stateIn(this, started = started, initialValue = realInitialValue)
}

public fun <T> CoroutineScopeOwner.derived(started: SharingStarted, observer: AutoRunCallback<T>): StateFlow<T> =
    scope.derived(started = started, observer = observer)
