package com.ensody.reactivestate

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope

/** Creates a [WorkQueue]. You have to manually call `consume()`. */
public fun <T> ViewModel.workQueue(): WorkQueue<T> = viewModelScope.workQueue<T>()

/** Creates a [WorkQueue]. You have to manually call `consume()`. */
public fun <T> LifecycleOwner.workQueue(): WorkQueue<T> = lifecycleScope.workQueue<T>()

/** Creates a [WorkQueue] for lambdas taking an argument. You have to manually call `consume()`. */
public fun <T> ViewModel.argWorkQueue(): WorkQueue<suspend (T) -> Unit> = viewModelScope.argWorkQueue<T>()

/** Creates a [WorkQueue] for lambdas taking a `this` argument. You have to manually call `consume()`. */
public fun <T> LifecycleOwner.thisWorkQueue(): WorkQueue<suspend T.() -> Unit> = lifecycleScope.thisWorkQueue<T>()

/** Creates a [WorkQueue] for lambdas taking a `this` argument. You have to manually call `consume()`. */
public fun <T> ViewModel.thisWorkQueue(): WorkQueue<suspend T.() -> Unit> = viewModelScope.thisWorkQueue<T>()

/** Creates a [WorkQueue] for lambdas taking an argument. You have to manually call `consume()`. */
public fun <T> LifecycleOwner.argWorkQueue(): WorkQueue<suspend (T) -> Unit> = lifecycleScope.argWorkQueue<T>()

/** Creates a [WorkQueue] and starts consuming it with the given [config]. */
public fun <T> ViewModel.workQueue(workers: Int = 1, config: WorkQueueConfigCallback<T>): WorkQueue<T> =
    viewModelScope.workQueue(workers = workers, config = config)

/** Creates a [WorkQueue] and starts consuming it with the given [config]. */
public fun <T> LifecycleOwner.workQueue(workers: Int = 1, config: WorkQueueConfigCallback<T>): WorkQueue<T> =
    lifecycleScope.workQueue(workers = workers, config = config)

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [worker]. */
public fun ViewModel.simpleWorkQueue(workers: Int = 1): WorkQueue<suspend () -> Unit> =
    viewModelScope.simpleWorkQueue(workers = workers)

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [worker]. */
public fun LifecycleOwner.simpleWorkQueue(workers: Int = 1): WorkQueue<suspend () -> Unit> =
    lifecycleScope.simpleWorkQueue(workers = workers)

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [conflatedWorker]. */
public fun ViewModel.conflatedWorkQueue(timeoutMillis: Long = 0L): WorkQueue<suspend () -> Unit> =
    viewModelScope.conflatedWorkQueue(timeoutMillis)

/** Creates a [WorkQueue] of simple lambdas and starts consuming it with [conflatedWorker]. */
public fun LifecycleOwner.conflatedWorkQueue(timeoutMillis: Long = 0L): WorkQueue<suspend () -> Unit> =
    lifecycleScope.conflatedWorkQueue(timeoutMillis)
