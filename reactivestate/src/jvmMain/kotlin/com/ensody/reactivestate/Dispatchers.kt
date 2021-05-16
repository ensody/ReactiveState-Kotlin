package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun getDispatchersIO(): CoroutineDispatcher =
    Dispatchers.IO
