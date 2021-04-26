package com.ensody.reactivestate

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotSameAs
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

internal class WhileUsedTest {
    @Test
    fun `WhileUsed tracks the usages`() = runBlockingTest {
        val nestedValue = WhileUsed { SomeData("hello") }
        val someValue = WhileUsed { nestedValue(it) }

        lateinit var data1: SomeData
        lateinit var data2: SomeData

        val completion = CompletableDeferred<Unit>()
        var job1 = launch {
            derived {
                data1 = get(someValue)
            }
            completion.await()
        }
        val job2 = launch {
            data2 = someValue(this)
            completion.await()
        }
        advanceUntilIdle()

        assertThat(data1).isSameAs(data2)

        val origData = data1
        job1.cancel()
        job1 = launch {
            data1 = someValue(this)
            completion.await()
        }
        advanceUntilIdle()

        assertThat(data1).isSameAs(origData)
        assertThat(data2).isSameAs(origData)

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
        assertThat(data1).isNotSameAs(origData)
    }

    @Test
    fun `WhileUsedReferenceToken scope`() = runBlockingTest {
        val lazyScope = WhileUsed { it.scope }
        val referenceToken = DisposableGroup()
        var scope = lazyScope.invoke(referenceToken)
        assertThat(scope.isActive).isTrue()
        referenceToken.dispose()
        assertThat(scope.isActive).isFalse()

        scope = lazyScope.invoke(referenceToken)
        assertThat(scope.isActive).isTrue()
        referenceToken.dispose()
        assertThat(scope.isActive).isFalse()
    }
}
