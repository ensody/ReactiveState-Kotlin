package com.ensody.reactivestate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class DITest {
    val someConfigFlag = MutableStateFlow(true)
    val testDI = DIImpl()

    init {
        testDI.register { FooDeps(get(someConfigFlag), barDeps, defaultDeps) }
        testDI.register { BarDeps(fooDeps) }
    }

    private val defaultFlow = testDI.derived { get(defaultDeps) }
    private val default = defaultFlow.value
    private val fooFlow = testDI.derived { get(fooDeps) }
    private val foo = fooFlow.value
    private val barFlow = testDI.derived { get(barDeps) }
    private val bar = barFlow.value

    @Test
    fun stability() {
        assertSame(default, testDI.derived { get(defaultDeps) }.value)
        assertSame(default, foo.defaultDeps)
        assertSame(foo, testDI.derived { get(fooDeps) }.value)
        assertSame(bar, testDI.derived { get(barDeps) }.value)
    }

    @Test
    fun tooEarlyAccess() {
        // Lazy access of course succeeds
        barFlow.value.configFlag

        // Accessing other deps non-lazily directly within the factory is forbidden
        testDI.register {
            // This causes a too early access
            get(barDeps)
            FooDeps(get(someConfigFlag), barDeps, defaultDeps)
        }
        assertFailsWith<IllegalStateException> { barFlow.value.configFlag }
    }

    @Test
    fun updateDIGraphOnRegister() {
        // Replacing FooDeps invalidates the whole subgraph depending on FooDeps. So, BarDeps gets re-created.
        testDI.register { FooDeps(get(someConfigFlag), barDeps, defaultDeps) }
        assertNotSame(foo, testDI.derived { get(fooDeps) }.value)
        assertSame(default, testDI.derived { get(fooDeps) }.value.defaultDeps)
        val newBar = testDI.derived { get(barDeps) }.value
        assertNotSame(bar, newBar)
        assertTrue(foo.circularConfigFlag)

        // Any flow also auto-updates
        assertSame(fooFlow.value, testDI.derived { get(fooDeps) }.value)
        assertSame(newBar, testDI.derived { get(barDeps) }.value)
        assertSame(barFlow.value, testDI.derived { get(barDeps) }.value)
    }

    @Test
    fun updateDIGraphOnStateFlowChange() {
        // Changing the StateFlow that FooDeps depends on also invalidates FooDeps and BarDeps
        someConfigFlag.value = false
        assertNotSame(foo, testDI.derived { get(fooDeps) }.value)
        assertNotSame(bar, testDI.derived { get(barDeps) }.value)

        // Any flow also auto-updates
        assertFalse(fooFlow.value.configFlag)
        assertFalse(fooFlow.value.circularConfigFlag)
        assertSame(fooFlow.value, testDI.derived { get(fooDeps) }.value)
        assertSame(barFlow.value, testDI.derived { get(barDeps) }.value)
    }
}

// -------------
// module DefaultDeps
// -------------

// Convenience accessor for FooDeps
private val DIResolver.defaultDeps: LazyProperty<DefaultDeps>
    get() = DI.run { get { DefaultDeps() } }

private class DefaultDeps(val value: Boolean = true)

// -------------
// module foo
// -------------

// Convenience accessor for FooDeps
private val DIResolver.fooDeps: LazyProperty<FooDeps> get() = DI.run { get() }

// The actual FooDeps DI module/node. This is a circular dependency with BarDeps below.
private class FooDeps(
    val configFlag: Boolean,
    lazyBarDeps: LazyProperty<BarDeps>,
    lazyDefaultDeps: LazyProperty<DefaultDeps>,
) {
    // All deps have to be resolved lazily
    val barDeps by lazyBarDeps
    val defaultDeps by lazyDefaultDeps

    // All deps have to be resolved lazily
    val circularConfigFlag by lazy { barDeps.configFlag }

    public fun myUseCase() = MyUseCase(circularConfigFlag)
}

private class MyUseCase(val circularConfigFlag: Boolean)

// -------------
// module bar
// -------------

// Convenience accessor for BarDeps
private val DIResolver.barDeps: LazyProperty<BarDeps> get() = DI.run { get() }

// Circular dependency to Foo, so we have
private class BarDeps(lazyFooDeps: LazyProperty<FooDeps>) {
    val fooDeps: FooDeps by lazyFooDeps

    val configFlag: Boolean by lazy { fooDeps.configFlag }
}
