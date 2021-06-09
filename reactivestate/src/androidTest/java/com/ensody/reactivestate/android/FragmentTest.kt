package com.ensody.reactivestate.android

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.test.*
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
internal class FragmentTest : BaseTest() {
    @Test
    fun `ReactiveState creation`() {
        val scenario = launchFragmentInContainer<TestFragment>()
        scenario.moveToState(Lifecycle.State.RESUMED)
        lateinit var viewModel: TestViewModel
        lateinit var fragment: TestFragment
        scenario.onFragment {
            fragment = it
            viewModel = it.viewModel
            it.viewModel.count.value = 10
            it.viewModel.throwError()
            assertIs<TestException>(it.errors.first())
            assertEquals(listOf<Any>(it.viewModel), it.attachedReactiveStates)
            assertSame(it.viewModel, it.attachedReactiveStates.first())
        }
        scenario.recreate()
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
