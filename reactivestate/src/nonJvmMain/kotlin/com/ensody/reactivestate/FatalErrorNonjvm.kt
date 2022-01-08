package com.ensody.reactivestate

import kotlinx.coroutines.CancellationException

public actual fun Throwable.isFatal(): Boolean =
    this is CancellationException
