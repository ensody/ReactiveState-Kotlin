package com.ensody.reactivestate

/**
 * Implement this interface to get notified of every [ReactiveState] added to your object.
 *
 * This can be useful e.g. to map [ReactiveState.loading] to a `setLoading(isLoading: Boolean)`
 * method in every UI screen (e.g. on Android you could have a `BaseFragment` implementing this interface).
 */
public interface OnReactiveStateAttached {
    public fun onReactiveStateAttached(reactiveState: ReactiveState<out ErrorEvents>)
}

public fun ReactiveState<out ErrorEvents>.attachTo(owner: Any) {
    if (owner is OnReactiveStateAttached) {
        owner.onReactiveStateAttached(this)
    }
}
