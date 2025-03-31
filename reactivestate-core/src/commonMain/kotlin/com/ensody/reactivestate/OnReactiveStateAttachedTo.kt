package com.ensody.reactivestate

/**
 * Implement this interface on your [ReactiveState] to get notified it's attached to a parent (screen or ReactiveState).
 */
public interface OnReactiveStateAttachedTo {
    public fun onReactiveStateAttachedTo(parent: Any)
}
