# Module reactivestate-android

APIs mostly useful for classic Android Fragment/Activity.

## Event handling

### Events

Events are modeled as simple interfaces where each event is a method:

```kotlin
// The ErrorEvents interface is already part of this library
interface ErrorEvents {
    // The onError event which contains the original exception
    fun onError(error: Throwable)
}

// Now a custom event type
interface FooEvents {
    // the onFoo event which contains a "name" argument
    fun onFoo(name: String)
    // the onOtherFoo event
    fun onOtherFoo()
}

// You can combine multiple events easily via multiple inheritance
interface CombinedEvents : FooEvents, ErrorEvents

// And of course you can also add more events
interface CombinedAndCustomEvents : FooEvents, ErrorEvents {
    fun onCustomEvent()
}
```

The last two examples show why events should be modeled as normal interfaces instead of sealed classes/interfaces.
With normal interfaces you can combine multiple event types very easily (even events defined outside of the current module).

In the next section we'll take a look at how those events can be triggered.

### EventNotifier

The `EventNotifier` class is an event queue on which you can emit events and some other part of your code can collect the events.
The `EventNotifier` is actually a `Channel` wrapped in a `Flow` interface.

Events are buffered until someone collects them.
This is important because you never want to lose events.
In contrast, a `SharedFlow` is lossy - which is often not what you want.

Example how to emit events:

```kotlin
// This EventNotifier can emit any events contained in CombinedEvents
val eventNotifier = EventNotifier<CombinedEvents>()

fun doSomething() {
    // Explicit version
    eventNotifier.tryEmit { onFoo("Slim Shady") }
    // Or the recommended, shorter version
    eventNotifier { onFoo("Slim Shady") }

    eventNotifier { onOtherFoo(e) }

    try {
        // ...
    } catch (e: Throwable) {
        eventNotifier { onError(e) }
    }
}
```

Example how you'd collect events:

```kotlin
// The event listener has to implement the respective events interface(s)
class MyEventListener(scope: CoroutineScope) : CombinedEvents {
    init {
        scope.launch {
            eventNotifier.handleEvents(this@MyEventListener)
        }
    }

    override fun onFoo(name: String) {
        // The onFoo event got triggered.
    }

    override fun onError(error: Throwable) {
        // The onError event got triggered. If MyEventListener is some UI screen
        // you'd probably show an error dialog here.
    }

    // ...
}
```

## Error handling

### ErrorEvents

This library provides a simple base events interface that is used in several places for error handling called `ErrorEvents`.

Here's the whole implementation

```kotlin
interface ErrorEvents {
    fun onError(error: Throwable)
}
```

Some of the functionality requires that you implement this interface.

### EventNotifier

Your ViewModels and other classes that can launch their own coroutines somehow have to communicate errors to the UI (or higher-level layers in general).

Note: This section only discusses the error-specific aspect of `EventNotifier`.
See [Event handling](event-handling.md) for more general usage of `EventNotifier`.

Imagine this in your business logic or ViewModel:

```kotlin
coroutineScope.launch {
    // someSuspendFun can throw an exception
    someSuspendFun()
}
```

If `someSuspendFun()` throws an exception, how will you let the user know that there is an error?
You need an event queue/dispatcher that is processed by the UI.
That's what `EventNotifier` is.

```kotlin
val eventNotifier = EventNotifier<ErrorEvents>()

fun doSomething() {
    coroutineScope.launch {
        try {
            someSuspendFun()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // This sends the event via eventNotifier
            eventNotifier { onError(e) }
        }
    }
}
```

You can simplify that code by using `withErrorReporting`:

```kotlin
val eventNotifier = EventNotifier<ErrorEvents>()

fun doSomething() {
    coroutineScope.launch {
        withErrorReporting(eventNotifier) {
            someSuspendFun()
        }
    }
}
```

### BaseReactiveState

The ViewModel base class [BaseReactiveState] already provides an `eventNotifier` and a `launch` function that catches exceptions:

```kotlin
class MyViewModel(scope: CoroutineScope) : BaseReactiveState<ErrorEvents>(scope) {
    init {
        launch {
            // ...code block...
        }
    }
}
```

This will automatically catch exceptions and trigger `eventNotifier { onError(throwable) }`.

## Correct lifecycle handling

```kotlin
interface MainEvents : ErrorEvents {
    fun showMessage(message: String)
}

// You can create a multiplatform ViewModel by deriving from
// BaseReactiveState instead. More details below.
class MainViewModel : ViewModel() {
    // This queue can be used to send events to the MainEvents in the STARTED
    // lifecycle state. Instead of boilerplaty event sealed classes we use a
    // simple MainEvents interface with methods.
    val eventNotifier = EventNotifier<MainEvents>()

    fun someAction() {
        viewModelScope.launch {
            val result = api.requestSomeAction()

            // Switch back to MainFragment (the latest visible instance).
            eventNotifier {
                // If the screen got rotated in the meantime, `this` would point
                // to the new MainFragment instance instead of the destroyed one
                // that did the initial `someAction` call above.
                showMessage(result.someMessage)
            }
        }
    }
}

class MainFragment : Fragment(), MainEvents {
    private val viewModel: MainViewModel by viewModels()

    init {
        // Execute the MainViewModel's events in the >=STARTED state to prevent crashes
        lifecycleScope.launchWhenStarted {
            viewModel.eventNotifier.collect { it() }
        }
    }

    // ...

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ...
        // val button = ...

        button.setOnClickListener {
            viewModel.someAction()
        }

        // ...
    }

    fun showMessage(message: String) {
        // ...
    }
}
```

On Android, managing operations independently of the UI lifecycle (e.g. button click -> request -> UI rotated -> response -> UI update/navigation) is made unnecessarily difficult because Android can destroy your UI in the middle of an operation.
To work around this, you'll usually launch a coroutine in `ViewModel.viewModelScope` and/or use a `Channel` to communicate between the `ViewModel` and the UI.

In order to simplify this pattern, ReactiveState provides `EventNotifier` and the lower-level `MutableFlow` (which has buffered, exactly-once consumption semantics like a `Channel`).

### Automatic cleanups based on lifecycle state

Especially on Android it's very easy to shoot yourself in the foot and e.g. have a closure that keeps a reference to a destroyed `Fragment` or mistakenly execute code on a destroyed UI.

ReactiveState provides a `Disposable` interface and most objects auto-dispose/terminate when a `CoroutineScope` or Android `Lifecycle` ends.
You can also use `disposable.disposeOnCompletionOf` to auto-dispose your disposables.
For more complex use-cases you can use `DisposableGroup` to combine (add/remove) multiple disposables into a single `Disposable` object.

With extension functions like `LifecycleOwner.onResume` or `LifecycleOwner.onStopOnce` you can easily add long-running or one-time observers to a `Lifecycle`.
These are the building blocks for your own lifecycle-aware components which can automatically clean up after themselves like `LifecycleOwner.autoRun` does.

Finally, with `validUntil()` you can define properties that only exist during a certain lifecycle subset and are dereference their value outside of that lifecycle subset.
This can get rid of the ugly [boilerplate](https://developer.android.com/topic/libraries/view-binding#fragments) when working with view bindings, for example.

## Multiplatform ViewModel example

This library allows creating multiplatform ViewModels (inherited from `BaseReactiveState`) and also provides a `by reactiveState` helper for attaching it to Android's `Activity` or `Fragment` with proper lifecycle handling.

```kotlin
// This is a multiplatform "ViewModel". It doesn't inherit from Android's ViewModel
// and doesn't depend on any Android code.
// It can persist saved instance state via StateFlowStore. On iOS you could pass
// e.g. an InMemoryStateFlowStore.
// The base class for such ViewModels (and other "living" stateful objects)
// is BaseReactiveState. You can alternatively use the ReactiveState interface
// e.g. together with delegation.
class MultiPlatformViewModel(
    scope: CoroutineScope,
    // The StateFlowStore allows for state restoration (like onSaveInstanceState).
    // See next section for details.
    private val store: StateFlowStore,
    // For dependency injection
    private val dependency: SomeDependency,
) : BaseReactiveState<MyEvents>(scope) {

    val data = MutableStateFlow("hello")

    fun doSomething() {
        // In contrast to scope.launch, the BaseReactiveState.launch function
        // automatically catches exceptions and forwards them to eventNotifier
        // via ErrorEvents.onError(throwable).
        launch {
            val result = callSomeSuspendFun()
            data.value = result
            // BaseReactiveState comes with a built-in eventNotifier
            eventNotifier { ... }
        }
    }
}

interface MyEvents : ErrorEvents {
    fun onResultReceived()
}

// Alternatively, this is an example in case you want to use Android-native ViewModels.
// This ViewModel can persist state with SavedStateHandle (no more onSaveInstanceState() boilerplate)
class StateViewModel(val handle: SavedStateHandle, dependency: SomeDependency) : ViewModel() {
    // ...
}

// Example integration with Android
class MainFragment : Fragment() {
    // Attaches a multiplatform ViewModel (ReactiveState) to the fragment.
    // Within the "by reactiveState" block you have access to scope and stateFlowStore which are taken from an
    // internally created Android ViewModel that hosts the ReactiveState instance.
    private val multiPlatformViewModel by reactiveState {
        MultiPlatformViewModel(scope, stateFlowStore, SomeDependency())
    }

    // Alternatively, for Android ViewModels there's stateViewModel and buildViewModel
    private val viewModel by stateViewModel { handle -> StateViewModel(handle, SomeDependency()) }

    // With buildOnViewModel you can create an arbitrary object that lives on an internally created wrapper ViewModel.
    // The "by reactiveState" helper is using this internally.
    private val someObjectOnAViewModel by buildOnViewModel { SomeObject() }
}
```

With `buildOnViewModel` you can create your fully custom ViewModel if prefer. However, `BaseReactiveState` comes with batteries included:

* event handling: Send one-time events to the UI via `eventNotifier`.
* error handling: `launch` catches all errors and forwards them to `eventNotifier` via `ErrorEvents.onError(throwable)`.
* lifecycle handling: With `by reactiveState` the `eventNotifier` is automatically observed in the `>= STARTED` state.
* loading indicators: `launch` automatically maintains a loading `StateFlow`, so you can show a loading indicator in the UI while the coroutine is running. This can use either the default `loading` or any custom `MutableValueFlow<Int>`, so you can distinguish different loading states, each having its own loading indicator in the UI.

For Android, ReactiveState's `by reactiveState`, `by buildViewModel`, `by stateViewModel`, `by buildOnViewModel`, and similar extension functions allow creating a `ViewModel` by directly instantiating it.
This results in more natural code and allows passing arguments to the `ViewModel`.
Internally, these helper functions are simple wrappers around `viewModels`, `ViewModelProvider.Factory` and `AbstractSavedStateViewModelFactory`.
They just reduce the amount of boilerplate for common use-cases.

### Launching coroutines

To give you a deeper understanding what happens when you run:

```kotlin
launch {
    // ...code block...
}
```

That piece of code is similar to writing this:

```kotlin
scope.launch {
    loading.atomicIncrement() // however that works
    try {
        // ...code block...
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        eventNotifier.invoke { onError(e) }  // explicitly writing invoke for clarity only
    } finally {
        loading.atomicDecrement() // however that works
    }
}
```

### Customizing `by reactiveState`

You can implement the `OnReactiveStateAttached` interface on your `Fragment`/`Activity` in order to customize the attachment procedure:

```kotlin
class MyFragment : Fragment(), OnReactiveStateAttached, ErrorEvents {
    val viewModel by reactiveState { ... }

    override fun onReactiveStateAttached(reactiveState: ReactiveState<out ErrorEvents>) {
        autoRun { setLoading(get(reactiveState.loading) > 0) }
    }

    fun setLoading(isLoading: Boolean) {
        // ...
    }
}
```

Alternatively, if you want to support multiple ViewModels and merge all their loadings states into one:

```kotlin
class MyFragment : Fragment(), OnReactiveStateAttached, ErrorEvents {
    // We'll merge the loading states of all ReactiveState instances into this one
    val loading = MutableValueFlow(0)

    val viewModel by reactiveState { ... }
    val viewModel2 by reactiveState { ... }
    val viewModel3 by reactiveState { ... }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        autoRun { setLoading(get(loading) > 0) }
    }

    fun setLoading(isLoading: Boolean) {
        // ...
    }

    override fun onReactiveStateAttached(reactiveState: ReactiveState<out ErrorEvents>) {
        lifecycleScope.launch {
            // Sum all loading states
            loading.incrementFrom(reactiveState.loading)
        }
    }
}
```
