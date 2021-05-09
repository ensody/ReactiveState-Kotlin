package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher

internal expect fun getDispatchersIO(): CoroutineDispatcher
