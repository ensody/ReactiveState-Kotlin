package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// TODO: Define a more I/O optimized thread pool
@Suppress("InjectDispatcher")
internal actual fun getDispatchersIO(): CoroutineDispatcher =
    Dispatchers.Default
