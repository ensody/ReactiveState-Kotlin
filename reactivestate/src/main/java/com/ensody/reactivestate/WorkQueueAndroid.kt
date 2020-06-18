package com.ensody.reactivestate

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope

/** Creates a [WorkQueue]. You have to manually call `consume()`. */
fun <T> ViewModel.workQueue() = viewModelScope.workQueue<T>()

/** Creates a [WorkQueue]. You have to manually call `consume()`. */
fun <T> LifecycleOwner.workQueue() = lifecycleScope.workQueue<T>()

/** Creates a [WorkQueue] for lambdas taking an argument. You have to manually call `consume()`. */
fun <T> ViewModel.argWorkQueue() = viewModelScope.argWorkQueue<T>()

/** Creates a [WorkQueue] for lambdas taking a `this` argument. You have to manually call `consume()`. */
fun <T> LifecycleOwner.thisWorkQueue() = lifecycleScope.thisWorkQueue<T>()

/** Creates a [WorkQueue] for lambdas taking a `this` argument. You have to manually call `consume()`. */
fun <T> ViewModel.thisWorkQueue() = viewModelScope.thisWorkQueue<T>()

/** Creates a [WorkQueue] for lambdas taking an argument. You have to manually call `consume()`. */
fun <T> LifecycleOwner.argWorkQueue() = lifecycleScope.argWorkQueue<T>()

/** Creates a [WorkQueue] and starts consuming it with the given [config]. */
fun <T> ViewModel.workQueue(workers: Int = 1, config: WorkQueueConfigCallback<T>) =
    viewModelScope.workQueue(workers = workers, config = config)

/** Creates a [WorkQueue] and starts consuming it with the given [config]. */
fun <T> LifecycleOwner.workQueue(workers: Int = 1, config: WorkQueueConfigCallback<T>) =
    lifecycleScope.workQueue(workers = workers, config = config)

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [worker]. */
fun ViewModel.simpleWorkQueue(workers: Int = 1) = viewModelScope.simpleWorkQueue(workers = workers)

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [worker]. */
fun LifecycleOwner.simpleWorkQueue(workers: Int = 1) =
    lifecycleScope.simpleWorkQueue(workers = workers)

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [conflatedWorker]. */
fun ViewModel.conflatedWorkQueue(timeoutMillis: Long = 0L) =
    viewModelScope.conflatedWorkQueue(timeoutMillis)

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [conflatedWorker]. */
fun LifecycleOwner.conflatedWorkQueue(timeoutMillis: Long = 0L) =
    lifecycleScope.conflatedWorkQueue(timeoutMillis)
