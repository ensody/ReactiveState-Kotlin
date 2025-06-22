package com.ensody.reactivestate

import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Locks the mutex with a while loop. WARNING: This blocks the current thread! */
public fun Mutex.spinLock() {
    while (true) {
        if (tryLock()) {
            break
        }
    }
}

/** Locks the mutex with [spinLock]. WARNING: This blocks the current thread! */
public fun <T> Mutex.withSpinLock(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    spinLock()
    try {
        return block()
    } finally {
        unlock()
    }
}
