package com.ensody.reactivestate.android

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ensody.reactivestate.test.AndroidCoroutineTest
import kotlinx.coroutines.test.runCurrent
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
internal class FragmentTest : AndroidCoroutineTest() {
    @Test
    fun creationOfReactiveState() = runTest {
        println("${Instant.now()} launchFragmentInContainer")
        val scenario = launchFragmentInContainer<TestFragment>()
        println("${Instant.now()} moveToState")
        scenario.moveToState(Lifecycle.State.RESUMED)
        lateinit var viewModel: TestViewModel
        lateinit var fragment: TestFragment
        println("${Instant.now()} onFragment")
        scenario.onFragment {
            println("${Instant.now()} within onFragment")
            fragment = it
            viewModel = it.viewModel
            it.viewModel.count.value = 10
            println("${Instant.now()} within onFragment runCurrent")
            runCurrent()
            println("${Instant.now()} within onFragment throwError")
            it.viewModel.throwError()
            println("${Instant.now()} within onFragment throwError runCurrent")
            runCurrent()
            println("${Instant.now()} within onFragment assertions")
            assertIs<TestException>(it.errors.first())
            assertEquals(listOf<Any>(it.viewModel), it.attachedReactiveStates)
            assertSame(it.viewModel, it.attachedReactiveStates.first())
            println("${Instant.now()} within onFragment done")
        }
        println("${Instant.now()} after onFragment runCurrent")
        runCurrent()
        println("${Instant.now()} scenario.recreate")
        scenario.recreate()
        println("${Instant.now()} after scenario runCurrent")
        runCurrent()
        println("${Instant.now()} scenario.onFragment")
        scenario.onFragment {
            println("${Instant.now()} within scenario.onFragment")
            assertNotSame(fragment, it)
            assertSame(viewModel, it.viewModel)
            assertEquals(10, it.viewModel.count.value)
            assertEquals(emptyList(), it.errors)
            assertEquals(listOf<Any>(it.viewModel), it.attachedReactiveStates)
            assertSame(it.viewModel, it.attachedReactiveStates.first())
            println("${Instant.now()} within scenario.onFragment done")
        }
    }
}
