package com.ensody.reactivestate

import com.ensody.reactivestate.test.ReactiveStateTest
import kotlinx.coroutines.CoroutineScope
import kotlin.test.assertEquals
import kotlin.test.Test

internal class BaseReactiveStateTest : ReactiveStateTest<ChildEvents>() {
    override val reactiveState by lazy {
        ParentViewModel(testCoroutineScope)
    }
    override val events = EventHandler()

    @Test
    fun `nesting of ReactiveStates`() = runBlockingTest {
        assertEquals(1, events.childEvents)
    }
}

internal class EventHandler : ChildEvents {
    var errors = mutableListOf<Throwable>()
    var childEvents = 0

    override fun onError(error: Throwable) {
        errors.add(error)
    }

    override fun onSomeChildEvent() {
        childEvents += 1
    }
}

internal class ParentViewModel(scope: CoroutineScope) : BaseReactiveState<ChildEvents>(scope) {
    val childViewModel by childReactiveState { ChildViewModel(scope) }
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
