package com.ensody.reactivestate

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

inline fun <reified T : Throwable> assertThrows(func: () -> Unit): T {
    try {
        func()
    } catch (e: Throwable) {
        if (e is T) {
            return e
        }
    }
    fail("Expected exception ${T::class.simpleName}")
}

@ExperimentalCoroutinesApi
class LifecycleTest {
    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun lifecycleObservers() = testDispatcher.runBlockingTest {
        val owner = object : LifecycleOwner {
            val lifecycle = LifecycleRegistry(this)
            var lifecycleValue: String by validUntil(::onStop)

            override fun getLifecycle(): Lifecycle = lifecycle
        }
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        var run = 0
        var start = 0
        var startOnce = 0
        var stop = 0
        var stopOnce = 0
        var resume = 0
        var resumeOnce = 0
        var pause = 0
        var pauseOnce = 0

        owner.autoRun {
            run += 1
        }
        owner.onStart {
            owner.lifecycleValue = "I have a value!"
            start += 1
        }
        owner.onStartOnce {
            startOnce += 1
        }
        owner.onStop {
            stop += 1
        }
        owner.onStopOnce {
            stopOnce += 1
        }
        owner.onResume {
            resume += 1
        }
        owner.onResumeOnce {
            resumeOnce += 1
        }
        owner.onPause {
            pause += 1
        }
        owner.onPauseOnce {
            pauseOnce += 1
        }

        assertThat(assertThrows<IllegalStateException> {
            owner.lifecycleValue
        }).isInstanceOf(IllegalStateException::class)
        assertThat(run).isEqualTo(0)
        assertThat(start).isEqualTo(0)
        assertThat(startOnce).isEqualTo(0)
        assertThat(stop).isEqualTo(0)
        assertThat(stopOnce).isEqualTo(0)
        assertThat(resume).isEqualTo(0)
        assertThat(resumeOnce).isEqualTo(0)
        assertThat(pause).isEqualTo(0)
        assertThat(pauseOnce).isEqualTo(0)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(owner.lifecycleValue).isEqualTo("I have a value!")
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(1)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stop).isEqualTo(0)
        assertThat(stopOnce).isEqualTo(0)
        assertThat(resume).isEqualTo(0)
        assertThat(resumeOnce).isEqualTo(0)
        assertThat(pause).isEqualTo(0)
        assertThat(pauseOnce).isEqualTo(0)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(owner.lifecycleValue).isEqualTo("I have a value!")
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(1)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stop).isEqualTo(0)
        assertThat(stopOnce).isEqualTo(0)
        assertThat(resume).isEqualTo(1)
        assertThat(resumeOnce).isEqualTo(1)
        assertThat(pause).isEqualTo(0)
        assertThat(pauseOnce).isEqualTo(0)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(owner.lifecycleValue).isEqualTo("I have a value!")
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(1)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stop).isEqualTo(0)
        assertThat(stopOnce).isEqualTo(0)
        assertThat(resume).isEqualTo(1)
        assertThat(resumeOnce).isEqualTo(1)
        assertThat(pause).isEqualTo(1)
        assertThat(pauseOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThrows<IllegalStateException> {
            owner.lifecycleValue
        }
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(1)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stop).isEqualTo(1)
        assertThat(stopOnce).isEqualTo(1)
        assertThat(resume).isEqualTo(1)
        assertThat(resumeOnce).isEqualTo(1)
        assertThat(pause).isEqualTo(1)
        assertThat(pauseOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(owner.lifecycleValue).isEqualTo("I have a value!")
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(2)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stop).isEqualTo(1)
        assertThat(stopOnce).isEqualTo(1)
        assertThat(resume).isEqualTo(1)
        assertThat(resumeOnce).isEqualTo(1)
        assertThat(pause).isEqualTo(1)
        assertThat(pauseOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(owner.lifecycleValue).isEqualTo("I have a value!")
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(2)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stop).isEqualTo(1)
        assertThat(stopOnce).isEqualTo(1)
        assertThat(resume).isEqualTo(2)
        assertThat(resumeOnce).isEqualTo(1)
        assertThat(pause).isEqualTo(1)
        assertThat(pauseOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(owner.lifecycleValue).isEqualTo("I have a value!")
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(2)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stop).isEqualTo(1)
        assertThat(stopOnce).isEqualTo(1)
        assertThat(resume).isEqualTo(2)
        assertThat(resumeOnce).isEqualTo(1)
        assertThat(pause).isEqualTo(2)
        assertThat(pauseOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThrows<IllegalStateException> {
            owner.lifecycleValue
        }
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(2)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stop).isEqualTo(2)
        assertThat(stopOnce).isEqualTo(1)
        assertThat(resume).isEqualTo(2)
        assertThat(resumeOnce).isEqualTo(1)
        assertThat(pause).isEqualTo(2)
        assertThat(pauseOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(owner.lifecycle.observerCount).isEqualTo(5)
    }
}
