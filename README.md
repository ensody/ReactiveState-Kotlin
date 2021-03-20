# ReactiveState for Kotlin and Android

[ ![Download](https://api.bintray.com/packages/ensody/maven/com.ensody.reactivestate%3Areactivestate/images/download.svg) ](https://bintray.com/ensody/maven/com.ensody.reactivestate%3Areactivestate/_latestVersion)

An easy to understand reactive state management solution for Kotlin and Android.

ReactiveState-Kotlin provides you with these foundations:

* reactive programming: everything is recomputed/updated automatically based on straightforward code
* demand-driven programming: resource-consuming computations and values are allocated on-demand and disposed when not needed
* event handling: simple events based on interfaces (more composable and less boilerplate than sealed classes)
* automatic error handling: no more forgotten try-catch or copy-pasted error handling logic all over the place
* automatic cleanup/dispose logic (e.g. dispose something once a `CoroutineScope` is canceled)
* coroutine-based unit tests: worry no more about passing around `CoroutineDispatcher`s everywhere
* lifecycle handling (esp. Android)
* state restoration (esp. Android)

This library is split into separate modules for Kotlin (`core` and `core-test`) and Android (`reactivestate`).

## Installation

Add the package to your `build.gradle`'s `dependencies {}`:

```groovy
dependencies {
    // Add the BOM using the desired ReactiveState version
    api platform("com.ensody.reactivestate:dependency-versions-bom:VERSION")

    // Now you can leave out the version number from all other ReactiveState modules:
    implementation "com.ensody.reactivestate:core" // For Kotlin-only projects
    implementation "com.ensody.reactivestate:reactivestate" // For Android projects

    implementation "com.ensody.reactivestate:core-test" // Utils for unit tests that want to use coroutines
}
```

Also, make sure you've integrated the Maven Central repo, e.g. in your root `build.gradle`:

```groovy
subprojects {
    repositories {
        // ...
        mavenCentral()
        // ...
    }
}
```

## Use-cases

### Keeping UI in sync with state

```kotlin
class MainViewModel : ViewModel() {
    // You can also use MutableLiveData, but then you'll have to deal with null.
    val name = MutableStateFlow("")
    val counter = MutableStateFlow(0)

    // This is automatically recomputed whenever counter's value is changed
    val doubledCounter: StateFlow<Int> = derived { 2 * get(counter) }

    // This is only computed while someone is subscribed to changes (autoRun, derived or collect)
    val onDemandDoubledCounter = derived(initial = 0, started = WhileSubscribed()) { 2 * get(counter) }

    fun increment() {
        counter.value += 1
    }
}

class MainFragment : Fragment() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ...
        // val nameInputField = ...
        // val incrementButton = ...

        nameInputField.addTextChangedListener {
            viewModel.name.value = nameInputField.text.toString()
        }

        autoRun {
            // get() returns the StateFlow.value (or LiveData.value) and tells autoRun to re-execute
            // this code block whenever model.name or model.counter is changed.
            // Result: isEnabled changes while you type.
            incrementButton.isEnabled = get(viewModel.name).isNotEmpty() && get(viewModel.doubledCounter) < 100
        }

        incrementButton.setOnClickListener {
            viewModel.increment()
        }

        // ...
    }
}
```

With `autoRun` (available on `LifecycleOwner`, `ViewModel`, `CoroutineScope`, etc.) you can observe and re-execute a function whenever any of the `StateFlow` or `LiveData` instances accessed by that function are modified.
On Android you can use this to keeping the UI in sync with your ViewModel. Of course, you can also keep non-UI state in sync.
Depending on the context in which `autoRun` is executed, this observer is automatically tied to a `CoroutineScope` (e.g. the `ViewModel`'s `viewModelScope`) or in case of a `Fragment`/`Activity` to the `onStart()`/`onStop()` lifecycle in order to prevent accidental crashes and unnecessary resource consumption.

With `derived` you can construct new `StateFlow`s based on the `autoRun` principle. You can control when the calculation should run by passing `Eagerly`, `Lazily` or `WhileSubscribed()`, for example. Especially `WhileSubscribed()` is important for expensive computations.

Note that `autoRun` can be extended to support observables other than `StateFlow`, `LiveData` and `WhileUsed` (see below).

### Correct lifecycle handling

```kotlin
interface MainEvents {
    fun showMessage(message: String)
}

class MainViewModel : ViewModel() {
    // This queue can be used to send events to the MainEvents in the STARTED lifecycle state.
    // Instead of boilerplaty event sealed classes we use a simple MainEvents interface with methods.
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

### Reference-counted / demand-driven singletons

```kotlin
val getCache = WhileUsed { mutableMapOf<String, Entity>() }
val getCacheProxy = WhileUsed { getCache(it) }  // example how to access other WhileUsed values

class MainViewModel : ViewModel() {
    // The cache is created here and disposed once the ViewModel is destroyed. If multiple ViewModels use the cache
    // at the same time then one single instance is shared between all of them and freed once the last ViewModel is
    // destroyed.
    private val cache: MutableMap<String, Entity> = getCacheProxy(viewModelScope)

    fun load(id: String) = cache[id]

    fun save(id: String, value: Entity) {
        cache[id] = value
    }

    // You can even track WhileUsed (with reference counting) via derived/autoRun.
    private val entities: StateFlow<Set<Entity>> = derived { get(getCacheProxy).keys }
}
```

`WhileUsed` allows you to create an on-demand computed singleton that gets disposed as soon as nobody is using it, anymore.
This can be used to e.g. share the same cache between all ViewModels within a certain screen flow, but free up the memory as soon as the user leaves the screen flow.

As an alternative to the `CoroutineScope` based reference counting you can also pass a `DisposableGroup` or use `WhileUsed.disposableValue()`, but then you mustn't forget to explicitly call `dispose()` once the value is not needed, anymore!

This is also a nice combination with `derived`/`stateIn`/`shareIn` and `WhileSubscribed`.

Note that `autoRun`/`derived` allow resolving `WhileUsed` via `get(myWhileUsed)` like you can with `StateFlow` and this correctly tracks the reference count (important for `derived` with `WhileSubscribed`).

### Automatic cleanups based on lifecycle state

Especially on Android it's very easy to shoot yourself in the foot and e.g. have a closure that keeps a reference to a destroyed `Fragment` or mistakenly execute code on a destroyed UI.

ReactiveState provides a `Disposable` interface and most objects auto-dispose/terminate when a `CoroutineScope` or Android `Lifecycle` ends.
You can also use `disposable.disposeOnCompletionOf` to auto-dispose your disposables.
For more complex use-cases you can use `DisposableGroup` to combine (add/remove) multiple disposables into a single `Disposable` object.

With extension functions like `LifecycleOwner.onResume` or `LifecycleOwner.onStopOnce` you can easily add long-running or one-time observers to a `Lifecycle`.
These are the building blocks for your own lifecycle-aware components which can automatically clean up after themselves like `LifecycleOwner.autoRun` does.

Finally, with `validUntil()` you can define properties that only exist during a certain lifecycle subset and are dereference their value outside of that lifecycle subset.
This can get rid of the ugly [boilerplate](https://developer.android.com/topic/libraries/view-binding#fragments) when working with view bindings, for example.

### Flexible ViewModel instantiation

```kotlin
class MainViewModel(dependency: SomeDependency) : ViewModel() {
    // ...
}

// This ViewModel can persist state with SavedStateHandle (no more onSaveInstanceState() boilerplate)
class StateViewModel(val handle: SavedStateHandle, dependency: SomeDependency) : ViewModel() {
    // ...
}

class MainFragment : Fragment() {
    private val viewModel by buildViewModel { MainViewModel(SomeDependency()) }
    private val viewModel2 by stateViewModel { handle -> StateViewModel(handle, SomeDependency()) }
}
```

ReactiveState's `buildViewModel`, `stateViewModel`, and similar extension functions allow creating a `ViewModel` by directly instantiating it.
This results in more natural code and allows passing arguments to the `ViewModel` (e.g. for dependency injection).
Internally, these helper functions are simple wrappers around `viewModels`, `ViewModelProvider.Factory` and `AbstractSavedStateViewModelFactory`.
They just reduce the amount of boilerplate for common use-cases.

### StateFlowStore - StateFlow based SavedStateHandle

```kotlin
class MainViewModel(handle: SavedStateHandle) : ViewModel() {
    val store = handle.stateFlowStore(viewModelScope)
    val count: StateFlow<Int> = store.getData("count", 0)
}
```

A `StateFlowStore` provides a similar API to `SavedStateHandle`, but based on `StateFlow` instead of `LiveData`.

With `InMemoryStateFlowStore` you can do e.g. unit testing or abstract away platform differences in multi-platform projects.

On Android you'll often want `SavedStateHandleStore` to convert `SavedStateHandle` to a `StateFlowStore`. There is also a convenient extension function: `SavedStateHandle.stateFlowStore(CoroutineScope)`

In practice, you'll want to make your ViewModel testable without Robolectric using a tiny indirection:

```kotlin
class MainViewModel(createStore: (CoroutineScope) -> StateFlowStore) : ViewModel() {
    // This indirection makes it possible to unit test with InMemoryStateFlowStore instead of SavedStateHandle
    val store = createStore(viewModelScope)
    val count = store.getData("count", 0)
}

class MainFragment : Fragment() {
    private val viewModel by stateViewModel { MainViewModel(it::stateFlowStore) }

    // ...
}
```

### Error handling

```kotlin
interface MyHandlerEvents : ErrorEvents {
    fun onSomethingHappened()
}

class MyHandler {
    val eventNotifier = EventNotifier<MyHandlerEvents>()

    fun doSomething() {
        withErrorHandling(eventNotifier) {
            if (computeResult() > 5) {
                eventNotifier { onSomethingHappened() }
            }
        }
    }
}
```

Since it's a common pattern, we provide `ErrorEvents` and `withErrorHandling` to automatically catch and report any errors within a code block to an `EventNotifier`.

The `ErrorEvents` interface provides a simple `onError(error: Throwable)` method.

This pattern is also useful in combination with `CoroutineLauncher` in order to automate error handling for all coroutines.

### MutableValueFlow - the more flexible alternative to MutableStateFlow

`MutableValueFlow` implements the same API as `MutableStateFlow`, but also provides an `update` method for working with mutable values:

```kotlin
// MutableValueFlow

valueFlow.update {
    it.subvalue1.deepsubvalue.somevalue += 3
    it.subvalue2.state = SomeState.IN_PROGRESS
    it.isLoading = true
}

// versus MutableStateFlow

stateFlow.value = flow.value.let {
    it.copy(
        subvalue1 = it.subvalue1.copy(
            deepsubvalue = it.subvalue1.deepsubvalue.copy(somevalue = it.subvalue1.deepsubvalue.somevalue + 3)
         ),
        subvalue2 = it.subvalue2.copy(state = SomeState.IN_PROGRESS),
        isLoading = true,
    )
}
```

If you work with immutable data classes then you might know this problem. You can make immutable data less painful with functional lenses (e.g. [arrow Optics DSL](https://arrow-kt.io/docs/optics/dsl/) and [arrow Lens](https://arrow-kt.io/docs/optics/lens/)), but that can still result in complicated and inefficient code.

On the other hand, mutable data does allow to shoot yourself in the foot. So whether you want to use `MutableValueFlow` is a question of your architecture and code structure.
Usually, reactive code consciously puts data into observables (`StateFlow`s) in order to allow for reactivity.
This results in a code structure where these `StateFlow`s are the primary hosts of each piece of data and the mutations are limited around each `StateFlow` or even around the observable database as the single source of truth.

So, under these circumstances it can be quite safe to work with mutable data and `MutableValueFlow` makes such use-cases simpler than `MutableStateFlow`.

### Unit tests with coroutines

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
        coroutinesTestRule.testCoroutineScope.launch {
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

If you can't derive from `CoroutineTest` directly (e.g. because you have some other base test class), you can alternatively use delegation with the `CoroutineTestRuleOwner` interface:

```kotlin
class MyTest : SomeBaseTestClass(), CoroutineTestRuleOwner by CoroutineTest() {
    @Test
    fun `some test`() = runBlockingTest {
        // ...
    }
}
```

If you want to go even lower-level there's also `CoroutineTestRule`:

```kotlin
class MyTest {
    @get:Rule
    override val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `some test`() = coroutineTestRule.runBlockingTest {
        // ...
    }
}
```

## See also

This library is based on [reactive_state](https://github.com/ensody/reactive_state) for Flutter and adapted to Kotlin and Android patterns.

## License

```
Copyright 2020-2021 Ensody GmbH, Waldemar Kornewald

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
