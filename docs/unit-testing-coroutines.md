# Unit tests with coroutines

The `CoroutineTest` base class provides some often useful helpers for working with coroutines.

```kotlin
class MyTest : CoroutineTest() {
    // This works because MainScope/Dispatchers.Main is automatically set up correctly by CoroutineTest
    val viewModel = MyViewModel()

    // Let's use a mock to test the events emitted by MyViewModel
    val events: MyEvents = mock()

    @Before
    fun setup() {
        // You can access the TestCoroutineScope directly to launch some background processing.
        // In this case, let's process MyViewModel's events.
        testCoroutineScope.launch {
            viewModel.eventNotifier.collect { events.it() }
        }
    }

    @Test
    fun `some test`() = runBlockingTest {
        viewModel.doSomething()
        advanceUntilIdle()
        verify(events).someEvent()
    }
}
```

This also sets up a global `dispatchers` variable which you can use in all of your code instead of passing a `CoroutineDispatcher` around as arguments:

```kotlin
// Use this instead of Dispatchers.IO. In unit tests this will automatically use
// the TestCoroutineDispatcher instead. Outside of unit tests it points to Dispatchers.IO.
// You can also define your own overrides if you want.
withContext(dispatchers.io) {
    // do some IO
}
```

If you can't derive from `CoroutineTest` directly (e.g. because you have some other base test class), you can alternatively use composition with the `CoroutineTestRule`:

```kotlin
class MyTest {
    val rule = CoroutineTestRule()

    @Test
    fun `some test`() = rule.runBlockingTest {
        // ...
    }
}
```
