package com.ensody.reactivestate

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class LifecycleTest {
    @Test
    fun lifecycleObservers() {
        val owner = object : LifecycleOwner {
            val lifecycle = LifecycleRegistry(this)

            override fun getLifecycle(): Lifecycle = lifecycle
        }
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        var run = 0
        var start = 0
        var stop = 0
        var startOnce = 0
        var stopOnce = 0

        owner.autoRun {
            run += 1
        }
        owner.onStart {
            start += 1
        }
        owner.onStop {
            stop += 1
        }
        owner.onStartOnce {
            startOnce += 1
        }
        owner.onStopOnce {
            stopOnce += 1
        }

        assertThat(run).isEqualTo(0)
        assertThat(start).isEqualTo(0)
        assertThat(stop).isEqualTo(0)
        assertThat(startOnce).isEqualTo(0)
        assertThat(stopOnce).isEqualTo(0)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(1)
        assertThat(stop).isEqualTo(0)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stopOnce).isEqualTo(0)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(1)
        assertThat(stop).isEqualTo(1)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stopOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(2)
        assertThat(stop).isEqualTo(1)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stopOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(run).isEqualTo(1)
        assertThat(start).isEqualTo(2)
        assertThat(stop).isEqualTo(2)
        assertThat(startOnce).isEqualTo(1)
        assertThat(stopOnce).isEqualTo(1)

        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(owner.lifecycle.observerCount).isEqualTo(2)
    }
}
