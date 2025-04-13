package com.ensody.reactivestate

import com.ensody.reactivestate.test.ReactiveStateTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

internal class BaseReactiveStateTest : ReactiveStateTest<ChildEvents>() {
    override val reactiveState by lazy { ParentViewModel(testScope.backgroundScope) }
    override val events = MockChildEvents()

    @Test
    fun nestingOfReactiveStates() = runTest {
        runCurrent()
        assertEquals(1, events.childCalls)
        reactiveState.increment()
        reactiveState.increment()
        runCurrent()
        assertEquals(4, reactiveState.doubled.value)
        assertEquals(4, reactiveState.lazyDoubled.value)
        assertEquals(2, reactiveState.countAutoRun.value)
        assertEquals(2, reactiveState.countCoAutoRun.value)
    }

    class MockChildEvents : MockErrorEvents(), ChildEvents {
        var childCalls: Int = 0

        override fun onSomeChildEvent() {
            childCalls += 1
        }
    }
}

internal class ParentViewModel(scope: CoroutineScope) : BaseReactiveState<ChildEvents>(scope) {
    val childViewModel by childReactiveState { ChildViewModel(scope) }

    val count = MutableStateFlow(0)
    val doubled = derived { get(count) * 2 }
    val lazyDoubled = derived(0) { get(doubled) }
    val countAutoRun = MutableStateFlow(0)
    val countCoAutoRun = MutableStateFlow(0)

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
