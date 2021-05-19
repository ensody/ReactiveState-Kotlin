package com.ensody.reactivestate.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.ensody.reactivestate.validUntil
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class LifecycleTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testCoroutineDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
    private val testCoroutineScope: TestCoroutineScope = TestCoroutineScope(testCoroutineDispatcher)

    init {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @Test
    fun `lifecycle observers`() = testCoroutineScope.runBlockingTest {
        var owner = MockLifecycleOwner()
        val lifecycle = MutableLiveData(owner)
        val fragment: Fragment = mockk {
            @Suppress("UNCHECKED_CAST")
            every { viewLifecycleOwnerLiveData } returns (lifecycle as LiveData<LifecycleOwner>)
        }

        var createView = 0
        var createViewOnce = 0
        var destroyView = 0
        var destroyViewOnce = 0

        val onCreateViewDisposable = fragment.onCreateView(this) { createView += 1 }
        fragment.onCreateViewOnce(this) { createViewOnce += 1 }
        val onDestroyViewDisposable = fragment.onDestroyView(this) { destroyView += 1 }
        fragment.onDestroyViewOnce(this) { destroyViewOnce += 1 }

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        assertEquals(1, createView)
        assertEquals(1, createViewOnce)
        assertEquals(0, destroyView)
        assertEquals(0, destroyViewOnce)

        lifecycle.value = null
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertEquals(1, createView)
        assertEquals(1, createViewOnce)
        assertEquals(1, destroyView)
        assertEquals(1, destroyViewOnce)

        var destroyViewOnce2 = 0
        fragment.onDestroyViewOnce(this) { destroyViewOnce2 += 1 }

        owner = MockLifecycleOwner()
        lifecycle.value = owner
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.value = null

        assertEquals(2, createView)
        assertEquals(1, createViewOnce)
        assertEquals(2, destroyView)
        assertEquals(1, destroyViewOnce)
        assertEquals(1, destroyViewOnce2)

        onCreateViewDisposable.dispose()
        onDestroyViewDisposable.dispose()

        owner = MockLifecycleOwner()
        lifecycle.value = owner
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.value = null

        assertEquals(2, createView)
        assertEquals(1, createViewOnce)
        assertEquals(2, destroyView)
        assertEquals(1, destroyViewOnce)
        assertEquals(1, destroyViewOnce2)

        owner = MockLifecycleOwner()
        lifecycle.value = owner

        var run = 0
        var start = 0
        var startOnce = 0
        var stop = 0
        var stopOnce = 0
        var resume = 0
        var resumeOnce = 0
        var pause = 0
        var pauseOnce = 0

        owner.autoRun { run += 1 }
        owner.onStart {
            owner.lifecycleValue = "I have a value!"
            start += 1
        }
        owner.onStartOnce { startOnce += 1 }
        owner.onStop { stop += 1 }
        owner.onStopOnce { stopOnce += 1 }
        owner.onResume { resume += 1 }
        owner.onResumeOnce { resumeOnce += 1 }
        owner.onPause { pause += 1 }
        owner.onPauseOnce { pauseOnce += 1 }

        assertFailsWith(IllegalStateException::class) {
            owner.lifecycleValue
        }
        assertEquals(0, run)
        assertEquals(0, start)
        assertEquals(0, startOnce)
        assertEquals(0, stop)
        assertEquals(0, stopOnce)
        assertEquals(0, resume)
        assertEquals(0, resumeOnce)
        assertEquals(0, pause)
        assertEquals(0, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertEquals("I have a value!", owner.lifecycleValue)
        assertEquals(1, run)
        assertEquals(1, start)
        assertEquals(1, startOnce)
        assertEquals(0, stop)
        assertEquals(0, stopOnce)
        assertEquals(0, resume)
        assertEquals(0, resumeOnce)
        assertEquals(0, pause)
        assertEquals(0, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertEquals("I have a value!", owner.lifecycleValue)
        assertEquals(1, run)
        assertEquals(1, start)
        assertEquals(1, startOnce)
        assertEquals(0, stop)
        assertEquals(0, stopOnce)
        assertEquals(1, resume)
        assertEquals(1, resumeOnce)
        assertEquals(0, pause)
        assertEquals(0, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertEquals("I have a value!", owner.lifecycleValue)
        assertEquals(1, run)
        assertEquals(1, start)
        assertEquals(1, startOnce)
        assertEquals(0, stop)
        assertEquals(0, stopOnce)
        assertEquals(1, resume)
        assertEquals(1, resumeOnce)
        assertEquals(1, pause)
        assertEquals(1, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertFailsWith(IllegalStateException::class) {
            owner.lifecycleValue
        }
        assertEquals(1, run)
        assertEquals(1, start)
        assertEquals(1, startOnce)
        assertEquals(1, stop)
        assertEquals(1, stopOnce)
        assertEquals(1, resume)
        assertEquals(1, resumeOnce)
        assertEquals(1, pause)
        assertEquals(1, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertEquals("I have a value!", owner.lifecycleValue)
        assertEquals(2, run)
        assertEquals(2, start)
        assertEquals(1, startOnce)
        assertEquals(1, stop)
        assertEquals(1, stopOnce)
        assertEquals(1, resume)
        assertEquals(1, resumeOnce)
        assertEquals(1, pause)
        assertEquals(1, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertEquals("I have a value!", owner.lifecycleValue)
        assertEquals(2, run)
        assertEquals(2, start)
        assertEquals(1, startOnce)
        assertEquals(1, stop)
        assertEquals(1, stopOnce)
        assertEquals(2, resume)
        assertEquals(1, resumeOnce)
        assertEquals(1, pause)
        assertEquals(1, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertEquals("I have a value!", owner.lifecycleValue)
        assertEquals(2, run)
        assertEquals(2, start)
        assertEquals(1, startOnce)
        assertEquals(1, stop)
        assertEquals(1, stopOnce)
        assertEquals(2, resume)
        assertEquals(1, resumeOnce)
        assertEquals(2, pause)
        assertEquals(1, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertFailsWith(IllegalStateException::class) {
            owner.lifecycleValue
        }
        assertEquals(2, run)
        assertEquals(2, start)
        assertEquals(1, startOnce)
        assertEquals(2, stop)
        assertEquals(1, stopOnce)
        assertEquals(2, resume)
        assertEquals(1, resumeOnce)
        assertEquals(2, pause)
        assertEquals(1, pauseOnce)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertEquals(5, owner.lifecycle.observerCount)
    }
}

internal class MockLifecycleOwner : LifecycleOwner {
    val lifecycle = LifecycleRegistry(this)
    var lifecycleValue: String by validUntil(::onStop)

    override fun getLifecycle(): Lifecycle = lifecycle
}
