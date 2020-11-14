package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Interface for a common set of [CoroutineDispatcher]s. */
public interface DispatcherConfig {
    /** A coroutine dispatcher that behaves like [Dispatchers.Main] (i.e. confined to the main UI thread). */
    public val main: CoroutineDispatcher
    /** A coroutine dispatcher that behaves like [Dispatchers.Default]. */
    public val default: CoroutineDispatcher
    /** A coroutine dispatcher that behaves like [Dispatchers.IO]. */
    public val io: CoroutineDispatcher
    /** A coroutine dispatcher that behaves like [Dispatchers.Unconfined]. */
    public val unconfined: CoroutineDispatcher
}

/** The default [DispatcherConfig], mapping to [Dispatchers]. */
public object DefaultDispatcherConfig : DispatcherConfig {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
}

/** The currently active [DispatcherConfig]. */
public var dispatchers: DispatcherConfig = DefaultDispatcherConfig
