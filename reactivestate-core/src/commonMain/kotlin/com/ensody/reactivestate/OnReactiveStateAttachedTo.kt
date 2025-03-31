package com.ensody.reactivestate

/**
 * Implement this interface to get notified when your [ReactiveState] is attached its parent (screen or ReactiveState).
 *
 * This can be useful e.g. to check if the parent
 */
public interface OnReactiveStateAttachedTo {
    public fun onReactiveStateAttachedTo(parent: Any)
}
