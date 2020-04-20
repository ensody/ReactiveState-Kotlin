package com.ensody.reactivestate

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class BindingTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun bindingOnFragment() {
        val scenario = launchFragmentInContainer<TestFragment>()

        // Unfortunately we have to make the fragment accessible outside of onFragment in order to
        // get readable assertThat tracebacks (otherwise the traceback line points to onFragment
        // instead of the failing assertThat line).
        lateinit var fragment: TestFragment
        scenario.onFragment { fragment = it }

        fragment.apply {
            bindTwoWay(state.name, textView)
            state.count.value = 1
            state.name.value = "test"
            assertThat(textView.text.toString()).isEqualTo(state.name.value)
            textView.text = "hello"
            assertThat(textView.text.toString()).isEqualTo(state.name.value)
        }

        // Bindings should auto-dispose themselves
        scenario.moveToState(Lifecycle.State.CREATED)
        fragment.apply {
            state.name.value = "test"
            assertThat(textView.text.toString()).isNotEqualTo(state.name.value)
        }

        // Re-creating the fragment should keep view model state
        scenario.recreate()
        scenario.onFragment { fragment = it }
        scenario.moveToState(Lifecycle.State.RESUMED)
        fragment.apply {
            bindTwoWay(state.name, textView)
            assertThat(state.name.value).isEqualTo("test")
            assertThat(textView.text.toString()).isEqualTo(state.name.value)
            assertThat(state.count.value).isEqualTo(1)
        }
    }
}
