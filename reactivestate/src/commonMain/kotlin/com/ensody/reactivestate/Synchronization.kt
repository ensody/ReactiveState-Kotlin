package com.ensody.reactivestate

import kotlinx.coroutines.sync.Mutex

/** Locks the mutex with a while loop. WARNING: This blocks the current thread! */
internal fun Mutex.spinLock() {
    while (true) {
        if (tryLock()) {
            break
        }
    }
}

/** Locks the mutex with [spinLock]. WARNING: This blocks the current thread! */
internal fun <T> Mutex.withSpinLock(block: () -> T): T {
    spinLock()
    try {
        return block()
    } finally {
        unlock()
    }
}
