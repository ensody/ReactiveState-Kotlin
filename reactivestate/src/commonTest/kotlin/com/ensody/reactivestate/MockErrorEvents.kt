package com.ensody.reactivestate

internal open class MockErrorEvents : ErrorEvents {
    val onErrorCalls = mutableListOf<Throwable>()

    override fun onError(error: Throwable) {
        onErrorCalls.add(error)
    }
}
