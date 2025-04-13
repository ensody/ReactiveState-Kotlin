package com.ensody.reactivestate.test

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule

/** A [CoroutineTest] that also sets up an [InstantTaskExecutorRule]. */
public open class AndroidCoroutineTest : CoroutineTest() {
    @get:Rule
    public val instantTaskExecutorRule: InstantTaskExecutorRule = InstantTaskExecutorRule()
}
