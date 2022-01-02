package com.ensody.reactivestate

import com.ensody.reactivestate.test.ReactiveStateTest
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlin.test.Test
import kotlin.test.assertEquals

internal class BaseReactiveStateTest : ReactiveStateTest<ChildEvents>() {
    override val reactiveState by lazy { ParentViewModel(testCoroutineScope) }
    override val events: ChildEvents = mockk(relaxed = true)

    @Test
    fun nestingOfReactiveStates() = runBlockingTest {
        verify { events.onSomeChildEvent() }
        reactiveState.increment()
        reactiveState.increment()
        assertEquals(4, reactiveState.doubled.value)
        assertEquals(4, reactiveState.lazyDoubled.value)
        assertEquals(2, reactiveState.countAutoRun.value)
        assertEquals(2, reactiveState.countCoAutoRun.value)
    }
}

internal class ParentViewModel(scope: CoroutineScope) : BaseReactiveState<ChildEvents>(scope) {
    val childViewModel by childReactiveState { ChildViewModel(scope) }

    val count = MutableValueFlow(0)
    val doubled = derived { get(count) * 2 }
    val lazyDoubled = derived(0) { get(doubled) }
    val countAutoRun = MutableValueFlow(0)
    val countCoAutoRun = MutableValueFlow(0)

    init {
        autoRun {
            countAutoRun.value = get(count)
        }
        coAutoRun {
            countCoAutoRun.value = get(count)
        }
    }

    fun increment() {
        count.increment()
    }
}

internal interface ChildEvents : ErrorEvents {
    fun onSomeChildEvent()
}

internal class ChildViewModel(scope: CoroutineScope) : BaseReactiveState<ChildEvents>(scope) {
    init {
        launch {
            eventNotifier {
                onSomeChildEvent()
            }
        }
    }
}
