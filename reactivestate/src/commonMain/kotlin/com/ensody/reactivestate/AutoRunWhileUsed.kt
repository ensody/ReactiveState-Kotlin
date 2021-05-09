package com.ensody.reactivestate

/** Returns [WhileUsed]'s value and keeps it alive as long as it's still used. */
public fun <T> Resolver.get(data: WhileUsed<T>): T {
    val value = track(data) { WhileUsedObservable(data) }.value
    require(value != null) { "Fatal error: The observer couldn't be started." }
    return value.value
}

private class WhileUsedObservable<T>(
    private val data: WhileUsed<T>,
) : AutoRunnerObservable {
    var value: DisposableValue<T>? = null

    override fun addObserver() {
        if (value == null) {
            value = data.disposableValue()
        }
    }

    override fun removeObserver() {
        value?.dispose()
    }
}
