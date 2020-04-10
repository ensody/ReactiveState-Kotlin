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
        assertThat(owner.lifecycle.observerCount).isEqualTo(4)
    }
}
