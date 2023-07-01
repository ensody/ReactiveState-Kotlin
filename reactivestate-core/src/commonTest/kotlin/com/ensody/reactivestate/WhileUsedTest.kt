package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class WhileUsedTest : CoroutineTest() {
    @Test
    fun ensureWhileUsedTracksTheUsages() = runTest {
        val nestedValue = WhileUsed { SomeData("hello") }
        val someValue = WhileUsed { nestedValue(it) }

        lateinit var data1: SomeData
        lateinit var data2: SomeData

        val completion = CompletableDeferred<Unit>()
        var job1 = launch {
            derived(synchronous = false) {
                data1 = get(someValue)
            }
            completion.await()
        }
        val job2 = launch {
            data2 = someValue(this)
            completion.await()
        }
        advanceUntilIdle()

        assertSame(data2, data1)

        val origData = data1
        job1.cancel()
        job1 = launch {
            data1 = someValue(this)
            completion.await()
        }
        advanceUntilIdle()

        assertSame(origData, data1)
        assertSame(origData, data2)

        // Now cancel all userScopes and launch a new requester
        job1.cancel()
        job2.cancel()
        job1 = launch {
            data1 = someValue(this)
            completion.await()
        }
        advanceUntilIdle()
        job1.cancel()

        // Now a new value is created
        assertNotSame(origData, data1)
    }

    @Test
    fun checkWhileUsedReferenceTokenScope() = runTest {
        val lazyScope = WhileUsed { it.scope }
        val referenceToken = DisposableGroup()
        var scope = lazyScope.invoke(referenceToken)
        assertTrue(scope.isActive)
        referenceToken.dispose()
        assertFalse(scope.isActive)

        scope = lazyScope.invoke(referenceToken)
        assertTrue(scope.isActive)
        referenceToken.dispose()
        assertFalse(scope.isActive)
    }
}
