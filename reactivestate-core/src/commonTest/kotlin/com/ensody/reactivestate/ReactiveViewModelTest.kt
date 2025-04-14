package com.ensody.reactivestate

import com.ensody.reactivestate.test.CoroutineTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ReactiveViewModelTest : CoroutineTest(ContextualValRoot()) {
    val viewModel by lazy { ParentReactiveViewModel(testScope.backgroundScope) }

    @Test
    fun nestingOfReactiveViewModels() = runTest {
        assertEquals(0, viewModel.loading.value)
        assertEquals(OnInit.State.Initializing, viewModel.onInit.state.value)
        viewModel.triggerOnInit()
        runCurrent()
        assertEquals(OnInit.State.Initializing, viewModel.onInit.state.value)
        assertEquals(1, viewModel.loading.value)
        viewModel.increment()
        viewModel.increment()
        runCurrent()
        assertEquals(4, viewModel.doubled.value)
        assertEquals(4, viewModel.lazyDoubled.value)
        assertEquals(2, viewModel.countAutoRun.value)
        assertEquals(2, viewModel.countCoAutoRun.value)

        assertEquals(1, viewModel.loading.value)
        advanceTimeBy(1000)
        assertEquals(0, viewModel.loading.value)
        assertEquals(OnInit.State.Finished, viewModel.onInit.state.value)
    }
}

internal class ParentReactiveViewModel(scope: CoroutineScope) : ReactiveViewModel(scope) {
    val childViewModel = ChildReactiveViewModel(scope)

    val count = MutableStateFlow(0)
    val doubled = derived { get(count) * 2 }
    val lazyDoubled = derived(0) { get(doubled) }
    val countAutoRun = MutableStateFlow(0)
    val countCoAutoRun = MutableStateFlow(0)

    init {
        onInit.observe {
            autoRun {
                countAutoRun.value = get(count)
            }
            coAutoRun {
                countCoAutoRun.value = get(count)
            }
        }
    }

    fun increment() {
        count.increment()
    }
}

internal class ChildReactiveViewModel(scope: CoroutineScope) : ReactiveViewModel(scope) {
    init {
        onInit.observe {
            launch {
                delay(999)
            }
        }
    }
}
