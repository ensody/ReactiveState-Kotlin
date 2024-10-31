package com.ensody.reactivestate

/**
 * Simple synchronous one-time event. Once [trigger] is called, subsequent [observe] calls are triggered immediately.
 *
 * This is not a stream of multiple events. That's what `Channel` and `Flow` would be used for, instead.
 * This kind of event can be activated exactly once and then it stays active forever.
 */
public interface OneTimeEvent<T> {
    public fun observe(block: T.() -> Unit)
    public fun unobserve(block: T.() -> Unit)
    public fun trigger(source: T)
}

public class DefaultOneTimeEvent<T> : OneTimeEvent<T> {
    private var source: T? = null
    private val observers: MutableList<T.() -> Unit> = mutableListOf()

    override fun observe(block: T.() -> Unit) {
        source?.block() ?: observers.add(block)
    }

    override fun unobserve(block: T.() -> Unit) {
        observers.remove(block)
    }

    override fun trigger(source: T) {
        if (this.source == null) {
            this.source = source
        }

        observers.forEach {
            it.invoke(source)
        }
        observers.clear()
    }
}
