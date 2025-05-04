package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Suppress("InjectDispatcher")
internal actual fun getDispatchersIO(): CoroutineDispatcher =
    Dispatchers.IO
