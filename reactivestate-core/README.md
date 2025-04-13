# Module reactivestate-core

Core APIs for multiplatform ViewModels ([ReactiveViewModel]), transforming `StateFlow`, intercepting the `MutableStateFlow` value setter, coroutine-locals (like thread-locals, or `CompositionLocal` for coroutines) and many other utilities.

## Deriving/transforming StateFlow

```kotlin
val number = MutableStateFlow(0)

// For complex apps you often need reactive StateFlows. That's what derived() is for.
// This StateFlow is automatically recomputed whenever number's value is changed.
// IMPORTANT: You have to use get(number) instead of number.value
val doubledNumber: StateFlow<Int> = derived { 2 * get(number) }

// Here we only compute the value while someone is subscribed to changes (autoRun,
// derived or collect). This can be important for expensive computations/operations.
val onDemandDoubledNumber = derived(initial = 0, started = WhileSubscribed()) {
    // Note: you could even call suspension functions from within this block
    // to e.g. fetch something from the backend.
    someSuspendFun(2 * get(number))
}
```

With [derived][com.ensody.reactivestate.derived] you can construct a new `StateFlow` based on one or multiple other `StateFlow` instances. It behaves similar to the Jetpack Compose `derivedStateOf`, but based on `StateFlow` and is thus more universal (e.g. can even be used with a native UI).

If you want to call suspend functions, you have to use `derived(initial = ...)` with an initial value. If you don't have access to a [CoroutineLauncher][com.ensody.reactivestate.CoroutineLauncher] (e.g. [ReactiveViewModel][com.ensody.reactivestate.ReactiveViewModel]) or `CoroutineScope` you can use [derivedWhileSubscribed][com.ensody.reactivestate.derivedWhileSubscribed]. This variant only recomputes while someone is subscribed.

One of the biggest advantages of `derived` is that with `StateFlow` your code becomes much more readable than `Flow` based chains of `combine` (with `Pair` or `Triple`), `map`, `flatMapLatest`, `conflate`, `stateIn`, etc. Also, `derived` helps you write more correct code, while `Flow` based chains can lead to difficult bugs (e.g. an innocent `filter` can lead to outdated values in the UI).

## Observing StateFlows

```kotlin
val base = MutableStateFlow(0)
val extra = MutableStateFlow(0)

autoRun {
    // This code block is re-executed whenever either of the numbers change.
    // IMPORTANT: You have to use get(base) instead of base.value
    if (get(base) + get(extra) > 10) {
        alert("You're flying too high")
    }
}
```

You can use [autoRun][com.ensody.reactivestate.autoRun] to observe one or multiple StateFlows in a similar way to Jetpack Compose. The lambda block passed to `autoRun` is re-executed whenever any of the StateFlows passed to `get()` is changed. As shown in the example, you can even use multiple `get()` calls to observe several StateFlows at once.

In classic Android Fragment/Activity code you can use `autoRun` to keeping the UI in sync with your ViewModel.
Depending on the context in which `autoRun` is executed, this observer is automatically tied to a `CoroutineScope` (e.g. the `ViewModel`'s `viewModelScope`) or in case of a `Fragment`/`Activity` to the `onStart()`/`onStop()` lifecycle in order to prevent accidental crashes and unnecessary resource consumption.

## Multiplatform ViewModels, loading state and error handling

```kotlin
class ExampleViewModel(scope: CoroutineScope, val repository: ExampleRepository) : ReactiveViewModel(scope) {
    val inputFieldValue = MutableStateFlow("default")

    fun submit() {
        // The launch function automatically catches exceptions and increments/decrements the loading indicator.
        // This way you can't forget the fundamentals that have to be always handled correctly.
        launch {
            repository.submit(inputFieldValue.value)
        }
    }
}

@Composable
fun MyScreen() {
    val viewModel = reactiveViewModel(onError = { /* ...show error dialog... */ }) {
        ExampleViewModel(scope)
    }
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    if (loading != 0) {
        // ...show loading overlay...
    }
    // ...render UI...
}
```

This module provides a new experimental [ReactiveViewModel][com.ensody.reactivestate.ReactiveViewModel] base class for ViewModels, but also the older [BaseReactiveState][com.ensody.reactivestate.BaseReactiveState] which is more work optimized for the older Android Fragment/Activity based UIs.

The differences compared to the androidx `ViewModel` class are:

* Convention for automatic error handling: Exceptions in coroutines are caught automatically and won't crash your whole app.
* Convention for loading indicators: There's a default [loading][com.ensody.reactivestate.CoroutineLauncher.loading] indicator used for all launched coroutines. You can also use multiple different indicators via `launch(withLoading = otherLoading) { ... }`.
* Supports coroutine-locals via [ContextualVal][com.ensody.reactivestate.ContextualVal] (see next section), scoped to the ViewModel's `CoroutineScope`.
* Gets the `CoroutineScope` as a constructor argument instead of creating its own. This might sound innocent/unnecessary, but it facilitates many useful things based on [ContextualVal][com.ensody.reactivestate.ContextualVal] like [OnInit] or nested child ViewModels that implicitly reuse the parent's ContextualVals.

## Coroutine-locals via ContextualVal (thread-locals / CompositionLocal for coroutines)

```kotlin
// A Boolean, defaulting to false, that can be overridden within the CoroutineContext
val ContextualIsFooEnabled = ContextualVal("ContextualIsFooEnabled") { false }

suspend fun foo() {
    // This retrieves the value from the CoroutineContext
    val isFooEnabled = ContextualIsFooEnabled.get()
    println(isFooEnabled)
}

suspend fun bar() {
    foo() // prints false
    ContextualIsFooEnabled.with(true) {
        foo() // prints true
    }
}

// Alternative access without suspend
println(ContextualIsFooEnabled.get(coroutineScope))
println(ContextualIsFooEnabled.get(coroutineContext))
```

With [ContextualVal][com.ensody.reactivestate.ContextualVal] you can define a `val` for which the value can be different depending on the coroutine and code block. This is somewhat similar to the Jetpack Compose `CompositionLocal`, but for coroutines.

IMPORTANT: The default value returned by the lambda block is not necessarily executed globally. Instead, the computed default value is attached to the root of the ViewModel's `CoroutineScope`. The global storage can be overridden by injecting [ContextualValRoot][com.ensody.reactivestate.ContextualValRoot] into the `CoroutineContext`. Each ViewModel instance has a `ContextualValRoot` injected by default.

Another example where a ViewModel provides optionally customizable feature-specific loading indicators:

```kotlin
class ExampleViewModel(scope: CoroutineScope) : ReactiveViewModel(scope) {
    val submitLoading = ContextualSubmitLoading.get(scope)

    fun submit() {
        launch(withLoading = submitLoading) {
            // ...
        }
    }

    companion object {
        // Define a feature specific loading indicator that defaults to the default `loading` indicator
        val ContextualSubmitLoading = ContextualVal("ContextualSubmitLoading") { ContextualLoading.get(it) }
    }
}

@Composable
fun MyScreen() {
    val viewModel = reactiveViewModel(onError = { /* ... */ }) {
        // We override submitLoading to be its own loading indicator instance instead of the default
        ExampleViewModel(scope + ExampleViewModel.ContextualSubmitLoading.valued { MutableStateFlow(0) })
    }
}

// Alternatively, during app initialization you could set a new default globally.
ExampleViewModel.ContextualSubmitLoading.default = { MutableStateFlow(0) }
```

## MutableStateFlow interceptors

You can use [beforeUpdate][com.ensody.reactivestate.beforeUpdate]/[afterUpdate][com.ensody.reactivestate.afterUpdate]/[withSetter][com.ensody.reactivestate.withSetter] on a `MutableStateFlow` to execute additional code on every
update.

If you prefer the following (at first unusual) approach, you can also reduce boilerplate in ViewModels because you can turn this:

```kotlin
private val _state = MutableStateFlow("value")
public val state: StateFlow<String> = _state.asStateFlow()

public fun updateState(value: String) {
    _state.value = value
    // ...some extra logic...
}
```

into this:

```kotlin
public val state: MutableStateFlow<String> = MutableStateFlow("value").afterUpdate {
    // ...some extra logic...
}
```

## Convert StateFlow to MutableStateFlow

You can use [toMutable][com.ensody.reactivestate.toMutable] to turn a `StateFlow` into a `MutableStateFlow`:

```kotlin
val readOnly: StateFlow<Int> = getSomeStateFlow()
val mutable: MutableStateFlow<Int> = readOnly.toMutable { value: Int ->
    // This is executed whenever someone sets `mutable.value = ...`.
}
```

You have to ensure that `readOnly` also gets updated somehow whenever `mutable.value` is assigned. The example in the next section will give you a better idea of the whole picture.

## Creating StateFlow and SharedFlow without CoroutineScope

The official coroutines library only provides `stateIn` and `shareIn` functions, but those take a `CoroutineScope`.
Sometimes you can pass the ViewModel's `CoroutineScope` through multiple layers of code (ugly), but often you need (singleton) `StateFlow` instances shared between multiple ViewModels.

This module provides [stateOnDemand][com.ensody.reactivestate.stateOnDemand] and [shareOnDemand][com.ensody.reactivestate.shareOnDemand] and [sharedFlow][com.ensody.reactivestate.sharedFlow] which only launch an internal coroutine while someone is subscribed, but can safely get garbage collected when nobody is subscribed, anymore.

For example, if you want to create a `getIntFlow()` function for a key-value store (e.g. `SharedPreferences` or `NSUserDefaults`) you might want to return the current result but also allow observing/collecting to receive updates.
With `stateOnDemand` you can create such a result and using `toMutable` you can make it mutable:

```kotlin
suspend fun KeyValueStore.getIntFlow(key: String, default: Int): MutableStateFlow<Int> =
    callbackFlow {
        // With callbackFlow we define the collect() behavior.
        // In this example, let's use a pseudo-API for getting notified whenever a key gets updated:
        val listener = KeyValueStore.OnChangeListener { changedKey: String ->
            if (changedKey == key) {
                send(getInt(key))
            }
        }
        addListener(listener)
        awaitClose { removeListener(listener) }
    }.stateOnDemand { previous: Wrapped<Int>? ->
        // stateOnDemand takes a getter function which defines the StateFlow.value behavior when nobody collects.
        // The previous value is also passed here, wrapped in a Wrapped() instance (which can be null if this is the
        // first value access). This can be useful for caching.
        getInt(key, default)
    }.toMutable { value: Int ->
        // toMutable defines the StateFlow.value = ... setter behavior
        putInt(key, value)
    }
```

Here we've turned a simple `Flow` (via `callbackFlow`) into a `StateFlow` that is safe for returning from a function
and we didn't need any `CoroutineScope`. Then we've used [toMutable] to turn that into a `MutableStateFlow`.

## State restoration

```kotlin
class MainViewModel(scope: CoroutineScope) : ReactiveViewModel(scope) {
    // The StateFlowStore is accessible from anywhere via the CoroutineScope/CoroutineContext
    val stateFlowStore: StateFlowStore = ContextualStateFlowStore.get(scope)
    val count: StateFlow<Int> = stateFlowStore.getData("count", 0)
}
```

A [StateFlowStore][com.ensody.reactivestate.StateFlowStore] provides a similar API to Android's `SavedStateHandle`, but based on `StateFlow` instead of `LiveData`.

There's also [InMemoryStateFlowStore][com.ensody.reactivestate.InMemoryStateFlowStore] which can be useful for unit tests.
