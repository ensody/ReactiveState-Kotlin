package com.ensody.reactivestate.android

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
internal class BindingTest {
    @Test
    fun bindingOnFragment() {
        val scenario = launchFragmentInContainer<TestFragment>()

        // Unfortunately we have to make the fragment accessible outside of onFragment in order to
        // get readable assertThat tracebacks (otherwise the traceback line points to onFragment
        // instead of the failing assertThat line).
        lateinit var fragment: TestFragment
        scenario.onFragment { fragment = it }

        fragment.run {
            bindTwoWay(viewModel.name, textView)
            viewModel.count.value = 1
            viewModel.name.value = "test"
            assertThat(textView.text.toString()).isEqualTo(viewModel.name.value)
            textView.text = "hello"
            assertThat("" + textView.text).isEqualTo(viewModel.name.value)
        }

        // Bindings should auto-dispose themselves
        scenario.moveToState(Lifecycle.State.CREATED)
        fragment.run {
            viewModel.name.value = "test"
            assertThat(textView.text.toString()).isNotEqualTo(viewModel.name.value)
        }

        // Re-creating the fragment should keep view model state
        scenario.recreate()
        scenario.onFragment { fragment = it }
        scenario.moveToState(Lifecycle.State.RESUMED)
        fragment.run {
            bindTwoWay(viewModel.name, textView)
            assertThat(viewModel.name.value).isEqualTo("test")
            assertThat(textView.text.toString()).isEqualTo(viewModel.name.value)
            assertThat(viewModel.count.value).isEqualTo(1)
        }
    }
}
