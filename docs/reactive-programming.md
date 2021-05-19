# Reactive programming

```kotlin
val number = MutableStateFlow(0)

// For complex apps you often need reactive StateFlows. That's what derived() is for.
// This StateFlow is automatically recomputed whenever number's value is changed
val doubledNumber: StateFlow<Int> = derived { 2 * get(number) }

// Here we only compute the value while someone is subscribed to changes (autoRun,
// derived or collect). This can be important for expensive computations/operations.
val onDemandDoubledNumber = derived(initial = 0, started = WhileSubscribed()) {
    // Note: you could even call suspension functions from within this block
    // to e.g. fetch something from the backend.
    2 * get(number)
}
```

With `autoRun` (available on `LifecycleOwner`, `ViewModel`, `CoroutineScope`, etc.) you can observe and re-execute a function whenever any of the `StateFlow` or `LiveData` instances accessed by that function are modified.
On Android you can use this to keeping the UI in sync with your ViewModel. Of course, you can also keep non-UI state in sync.
Depending on the context in which `autoRun` is executed, this observer is automatically tied to a `CoroutineScope` (e.g. the `ViewModel`'s `viewModelScope`) or in case of a `Fragment`/`Activity` to the `onStart()`/`onStop()` lifecycle in order to prevent accidental crashes and unnecessary resource consumption.

With `derived` you can construct new `StateFlow`s based on the `autoRun` principle. You can control when the calculation should run by passing `Eagerly`, `Lazily` or `WhileSubscribed()`, for example. Especially `WhileSubscribed()` is important for expensive computations.

Note that `autoRun` can be extended to support observables other than `StateFlow`, `LiveData` and `WhileUsed`.

The simplicity advantage of `autoRun`/`derived` requires using `StateFlow` instead of `Flow` to avoid writing chains of `combine`, `map`, `flatMapLatest`, `conflate`, etc.
Luckily, if you only have a `Flow`, you can use `Flow.stateIn()` to convert it to a `StateFlow`.
