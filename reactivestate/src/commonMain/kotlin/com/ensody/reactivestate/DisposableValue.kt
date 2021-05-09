package com.ensody.reactivestate

/** A [value] that must be explicitly [dispose]d when it's not needed anymore. */
public class DisposableValue<T>(public val value: T, private val disposer: () -> Unit): Disposable {
    override fun dispose() {
        disposer()
    }
}
