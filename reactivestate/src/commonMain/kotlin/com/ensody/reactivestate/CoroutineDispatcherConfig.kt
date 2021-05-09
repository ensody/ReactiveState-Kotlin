package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Interface for a common set of [CoroutineDispatcher]s. */
public interface CoroutineDispatcherConfig {
    /** A coroutine dispatcher that behaves like [Dispatchers.Main] (i.e. confined to the main UI thread). */
    public val main: CoroutineDispatcher
    /** A coroutine dispatcher that behaves like [Dispatchers.Default]. */
    public val default: CoroutineDispatcher
    /** A coroutine dispatcher that behaves like [Dispatchers.IO]. */
    public val io: CoroutineDispatcher
    /** A coroutine dispatcher that behaves like [Dispatchers.Unconfined]. */
    public val unconfined: CoroutineDispatcher
}

/** The default [CoroutineDispatcherConfig], mapping to [Dispatchers]. */
public object DefaultCoroutineDispatcherConfig : CoroutineDispatcherConfig {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val io: CoroutineDispatcher get() = getDispatchersIO()
    override val unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
}

/** The currently active [CoroutineDispatcherConfig]. */
public var dispatchers: CoroutineDispatcherConfig = DefaultCoroutineDispatcherConfig
