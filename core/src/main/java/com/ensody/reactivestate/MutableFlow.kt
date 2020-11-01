package com.ensody.reactivestate

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A [Flow] where you can [emit]/[tryEmit] values into (backed by a [Channel]).
 */
public interface MutableFlow<T> : Flow<T> {
    /** Adds a value to this Flow if there's still capacity left. */
    public fun tryEmit(value: T)

    /** Adds a value to this Flow if there's still capacity left or suspends until the value can be added. */
    public suspend fun emit(value: T)
}

/** Creates a [MutableFlow]. */
@Suppress("FunctionName")
public fun <T> MutableFlow(
    capacity: Int = Channel.RENDEZVOUS,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
): MutableFlow<T> =
    MutableFlowImpl(
        Channel(
            capacity = capacity,
            onBufferOverflow = onBufferOverflow,
        )
    )

private class MutableFlowImpl<T>(
    private val flow: Channel<T>,
) : MutableFlow<T>, Flow<T> by flow.receiveAsFlow() {

    override fun tryEmit(value: T) {
        flow.offer(value)
    }

    override suspend fun emit(value: T) {
        flow.send(value)
    }
}
