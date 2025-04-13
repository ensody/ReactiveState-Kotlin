package com.ensody.reactivestate

import kotlinx.coroutines.CancellationException

public actual fun Throwable.isFatal(): Boolean =
    this is VirtualMachineError ||
        this is ThreadDeath ||
        this is InterruptedException ||
        this is LinkageError ||
        this is CancellationException
