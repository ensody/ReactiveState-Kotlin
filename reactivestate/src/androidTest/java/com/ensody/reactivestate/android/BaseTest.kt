package com.ensody.reactivestate.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule

internal abstract class BaseTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    val testCoroutineDispatcher = TestCoroutineDispatcher()
    val testCoroutineScope = TestCoroutineScope(testCoroutineDispatcher)

    init {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    open fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) =
        try {
            testCoroutineScope.runBlockingTest(block)
        } finally {
            testCoroutineScope.cleanupTestCoroutines()
        }
}
