package com.ensody.reactivestate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A [StateFlow] which can retrieve a new value e.g. from a backend via [refresh].
 */
@ExperimentalReactiveStateApi
public interface RefreshableStateFlow<T> : StateFlow<T> {
    /**
     * Refreshes the [value].
     *
     * @param force Can enforce refreshing the value and bypassing any caching mechanism.
     */
    public suspend fun refresh(force: Boolean = false)
}

@ExperimentalReactiveStateApi
public fun <T> MutableStateFlow<T>.withRefresh(
    block: suspend MutableStateFlow<T>.(force: Boolean) -> Unit,
): RefreshableStateFlow<T> =
    DefaultRefreshableStateFlow(this) { block(it) }

@ExperimentalReactiveStateApi
public fun <T> StateFlow<T>.withRefresh(
    block: suspend StateFlow<T>.(force: Boolean) -> Unit,
): RefreshableStateFlow<T> =
    DefaultRefreshableStateFlow(this) { block(it) }

private class DefaultRefreshableStateFlow<T>(
    private val delegate: StateFlow<T>,
    private val refresher: suspend (force: Boolean) -> Unit,
) : RefreshableStateFlow<T>, StateFlow<T> by delegate {
    override suspend fun refresh(force: Boolean) {
        refresher(force)
    }
}
