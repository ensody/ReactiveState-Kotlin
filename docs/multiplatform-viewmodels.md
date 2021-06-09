# Multiplatform ViewModels

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

## Launching coroutines

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

## Customizing `by reactiveState`

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
