package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlin.coroutines.CoroutineContext

/**
 * Contains all values which are part of the ViewModel build process.
 */
@ExperimentalReactiveStateApi
public data class ReactiveStateContext(public val scope: CoroutineScope) {
    public operator fun plus(element: CoroutineContext.Element): ReactiveStateContext =
        copy(scope = scope + element)
}
