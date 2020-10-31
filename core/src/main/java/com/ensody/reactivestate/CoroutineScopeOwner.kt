package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope

/** Common interface for all classes holding a CoroutineScope. */
public interface CoroutineScopeOwner {
    public val scope: CoroutineScope
}
