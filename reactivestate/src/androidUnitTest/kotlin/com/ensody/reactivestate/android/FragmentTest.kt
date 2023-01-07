package com.ensody.reactivestate.android

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ensody.reactivestate.test.AndroidCoroutineTest
import kotlinx.coroutines.test.runCurrent
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
internal class FragmentTest : AndroidCoroutineTest() {
    @Test
    fun creationOfReactiveState() = runTest {
        val scenario = launchFragmentInContainer<TestFragment>()
        scenario.moveToState(Lifecycle.State.RESUMED)
        lateinit var viewModel: TestViewModel
        lateinit var fragment: TestFragment
        scenario.onFragment {
            fragment = it
            viewModel = it.viewModel
            it.viewModel.count.value = 10
            runCurrent()
            it.viewModel.throwError()
            runCurrent()
            assertIs<TestException>(it.errors.first())
            assertEquals(listOf<Any>(it.viewModel), it.attachedReactiveStates)
            assertSame(it.viewModel, it.attachedReactiveStates.first())
        }
        runCurrent()
        scenario.recreate()
        runCurrent()
        scenario.onFragment {
            assertNotSame(fragment, it)
            assertSame(viewModel, it.viewModel)
            assertEquals(10, it.viewModel.count.value)
            assertEquals(emptyList(), it.errors)
            assertEquals(listOf<Any>(it.viewModel), it.attachedReactiveStates)
            assertSame(it.viewModel, it.attachedReactiveStates.first())
        }
    }
}
