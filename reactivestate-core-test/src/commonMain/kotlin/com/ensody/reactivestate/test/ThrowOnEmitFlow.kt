package com.ensody.reactivestate.test

import com.ensody.reactivestate.MutableFlow

public class ThrowOnEmitFlow : MutableFlow<Throwable> by MutableFlow() {
    override fun tryEmit(value: Throwable): Boolean {
        throw value
    }

    override suspend fun emit(value: Throwable) {
        throw value
    }
}
