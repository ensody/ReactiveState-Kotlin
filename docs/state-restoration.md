# State restoration

```kotlin
// Multiplatform ViewModel

class MainViewModel(scope: CoroutineScope, store: StateFlowStore) :
    BaseReactiveState<ErrorEvents>(scope) {

    val count: StateFlow<Int> = store.getData("count", 0)
}

// Android ViewModel

class MainViewModel(store: StateFlowStore) : ViewModel() {
    val count: StateFlow<Int> = store.getData("count", 0)
}
```

A `StateFlowStore` provides a similar API to Android's `SavedStateHandle`, but based on `StateFlow` instead of `LiveData`.

With `InMemoryStateFlowStore` you can do e.g. unit testing or abstract away platform differences in multiplatform projects.

This is how you can create an Android ViewModel with a `StateFlowStore`:

```kotlin
// Multiplatform ViewModel

class MainFragment : Fragment() {
    private val viewModel by reactiveState { MainViewModel(scope, stateFlowStore) }

    // ...
}

// Android ViewModel

class MainFragment : Fragment() {
    private val viewModel by stateFlowViewModel { MainViewModel(stateFlowStore) }

    // ...
}
```

Both `by reactiveState` and `by stateFlowViewModel` provide a `stateFlowStore` value within their lambda blocks.
