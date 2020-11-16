# ReactiveState for Kotlin and Android

[ ![Download](https://api.bintray.com/packages/ensody/maven/com.ensody.reactivestate%3Areactivestate/images/download.svg) ](https://bintray.com/ensody/maven/com.ensody.reactivestate%3Areactivestate/_latestVersion)

An easy to understand reactive state management solution for Kotlin and Android.

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

Also, make sure you've integrated the Maven repos, e.g. in your root `build.gradle`:

```groovy
allprojects {
    repositories {
        // ...
        jcenter()
        maven {
            url "https://dl.bintray.com/ensody/maven/"
        }
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

        bindTwoWay(viewModel.name, nameInputField)

        autoRun {
            // get() returns the StateFlow.value (or LiveData.value) and tells autoRun to re-execute
            // this code block whenever model.name or model.counter is changed.
            // Result: isEnabled changes while you type.
            incrementButton.isEnabled = get(viewModel.name).isNotEmpty() && get(viewModel.counter) < 100
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

With `bind` and `bindTwoWay` you can easily create one-way or two-way bindings between `StateFlow`/`LiveData` and your views.
These bindings are automatically tied to the `onStart()`/`onStop()` lifecycle of your `Fragment`/`Activity` (same as with `autoRun`).

Note that `autoRun` and `bind` can be extended to support observables other than `StateFlow` and `LiveData`.

### Correct lifecycle handling

```kotlin
interface MainView {
    fun showMessage(message: String)
}

class MainViewModel : ViewModel() {
    // This queue can be used to send events to the MainView in the STARTED lifecycle state.
    // Instead of boilerplaty event classes we use a simple MainView interface with methods.
    val viewExecutor = EventNotifier<MainView>()

    fun someAction() {
        viewModelScope.launch {
            val result = api.requestSomeAction()

            // Switch back to MainFragment (the latest visible instance).
            viewExecutor {
                // If the screen got rotated in the meantime, `this` would point
                // to the new MainFragment instance instead of the destroyed one
                // that did the initial `someAction` call above.
                showMessage(result.someMessage)
            }
        }
    }
}

class MainFragment : Fragment(), MainView {
    private val viewModel: MainViewModel by viewModels()

    init {
        // Safely execute the MainViewModel's events in the >=STARTED state
        lifecycleScope.launchWhenStarted {
            viewModel.viewExecutor.collect { it() }
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
For more complex use-cases you can use `DisposableGroup` to combine (add/remove) multiple disposables into a single disposable object.

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

### Unit tests with coroutines

The `CoroutineTest` base class provides some often useful helpers for working with coroutines.

```kotlin
class MyTest : CoroutineTest() {
    // This works because MainScope/Dispatchers.Main is automatically set up correctly by CoroutineTest
    val viewModel = MyViewModel()

    // Let's use a mock to test the events emitted by MyViewModel
    val view: MyView = mock()

    @Before
    fun setup() {
        // You can access the TestCoroutineScope directly to launch some background processing.
        // In this case, let's process MyViewModel's events.
        coroutinesTestRule.testCoroutineScope.launch {
            viewModel.viewExecutor.collect { view.it() }
        }
    }

    @Test
    fun `some test`() = runBlockingTest {
        viewModel.doSomething()
        advanceUntilIdle()
        verify(view).someEvent()
    }
}
```

This also sets up a global `dispatchers` variable which you can use in all of your code instead of passing a `CoroutineDispatcher` around as arguments:

```kotlin
// Use this instead of Dispatchers.IO. In unit tests this will automatically use
// the TestCoroutineScope instead. Outside of unit tests it points to Dispatchers.IO.
// You can also define your own overrides if you want.
withContext(dispatchers.io) {
    // do some IO
}
```

As an alternative to `CoroutineTest`, you can also implement the `CoroutineTestRuleOwner` interface (e.g. if you have some existing base class):

```kotlin
class MyTest : SomeBaseTestClass(), CoroutineTestRuleOwner {
    override val coroutineTestRule = CoroutineTestRule()

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

## Examples

Example `ViewModel`:

```kotlin
class MainViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val store = SavedStateHandleStore(viewModelScope, savedStateHandle)

    val count = store.getData("count", 0)

    // These store form data
    val username = store.getData("username", "")
    val password = store.getData("password", "")
    val usernameError = MutableStateFlow("")
    val passwordError = derived {
        validatePassword(get(password))
    }

    // Simple form validation with multiple fields
    val isFormValid = derived {
        get(username).isNotEmpty() && get(usernameError).isEmpty() &&
        get(password).isNotEmpty() && get(passwordError).isEmpty()
    }

    init {
        // Instead of derived you can also use autoRun for more complex
        // cases (e.g. if you need to set multiple StateFlow/LiveData values or
        // you want to deal with coroutines and throttling/debouncing).
        autoRun {
            usernameError.value = validateUsername(get(username))
        }
    }

    fun increment() {
        count.value += 1
    }
}
```

Example `Fragment` (using view bindings for type safety):

```kotlin
class MainFragment : Fragment() {
    private val viewModel by stateViewModel { MainViewModel(it) }
    private var binding by validUntil<MainFragmentBinding>(::onDestroyView)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)

        // Two-way bindings (String/TextView or Bool/CompoundButton)
        bindTwoWay(viewModel.username, binding.username)
        bindTwoWay(viewModel.password, binding.password)

        // One-way bindings (also possible in the other direction)
        bind(binding.usernameError, viewModel.usernameError)
        bind(binding.passwordError, viewModel.passwordError)

        // One-way binding using more flexible autoRun callback style.
        bind(binding.count) {
            // NOTE: You'll probably want to localize this string.
            "${get(viewModel.count)}"
        }

        // Even more complicated cases can use autoRun directly.
        autoRun {
            // Only enable submit button if form is valid
            binding.submitButton.isEnabled = get(viewModel.isFormValid)
        }

        autoRun {
            val invalid = get(viewModel.usernameError).isNotEmpty()
            // Show username error TextView only when there is an error
            binding.usernameError.visibility =
                if (invalid) View.VISIBLE else View.GONE
        }

        binding.increment.setOnClickListener {
            viewModel.increment()
        }

        return binding.root
    }
}
```

## See also

This library is based on [reactive_state](https://github.com/ensody/reactive_state) for Flutter and adapted to Kotlin and Android patterns.
