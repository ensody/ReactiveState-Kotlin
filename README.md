# ReactiveState for Kotlin and Android

[ ![Download](https://api.bintray.com/packages/ensody/maven/com.ensody.reactivestate%3Areactivestate/images/download.svg) ](https://bintray.com/ensody/maven/com.ensody.reactivestate%3Areactivestate/_latestVersion)

An easy to understand reactive state management solution for Kotlin and Android.

This library is split into two separate modules for Kotlin ([`core`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/)) and Android ([`reactivestate`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/)).

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

With [`autoRun`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/auto-run/) (available on `LifecycleOwner`, `ViewModel`, [`Scoped`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/com.ensody.reactivestate/-scoped/), `CoroutineScope`, etc.)
you can observe and re-execute a function whenever any of the `StateFlow` or `LiveData` instances accessed by that function are modified.
On Android you can use this to keeping the UI in sync with your ViewModel. Of course, you can also keep non-UI state in sync.
Depending on the context in which `autoRun` is executed, this observer is automatically tied to a `CoroutineScope` (e.g. the `ViewModel`'s `viewModelScope`) or in case of a `Fragment`/`Activity` to the `onStart()`/`onStop()` lifecycle in order to prevent accidental crashes and unnecessary resource consumption.

With [`bind`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/bind/)
and [`bindTwoWay`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/bind-two-way/) you can easily create one-way or two-way bindings between `StateFlow`/`LiveData` and your views.
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
            viewExecutor.launch {
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
        viewModel.viewExecutor.consume(this, this)
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

In order to simplify this pattern, ReactiveState provides [`EventNotifier`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/com.ensody.reactivestate/-event-notifier/).

### Automatic cleanups based on lifecycle state

Especially on Android it's very easy to shoot yourself in the foot and e.g. have a closure that keeps a reference to a destroyed `Fragment` or mistakenly execute code on a destroyed UI.

ReactiveState provides a [`Disposable`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/com.ensody.reactivestate/-disposable/) interface and most objects auto-dispose/terminate when a `CoroutineScope` or Android `Lifecycle` ends.
You can also use [`disposable.disposeOnCompletionOf`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/com.ensody.reactivestate/kotlinx.coroutines.-disposable-handle/dispose-on-completion-of/) to auto-dispose your disposables.
For more complex use-cases you can use [`DisposableGroup`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/com.ensody.reactivestate/-disposable-group/) (which is a `Disposable`) to group multiple disposables into a single disposable object.

With extension functions like [`LifecycleOwner.onResume`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/on-resume/)
or [`LifecycleOwner.onStopOnce`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/on-stop-once/)
you can easily add long-running or one-time observers to a `Lifecycle`.
These are the building blocks for your own lifecycle-aware components which can automatically clean up after themselves like
[`LifecycleOwner.autoRun`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/auto-run/)
does.

Also, you can use extension functions like
[`LifecycleOwner.launchWhileStarted`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/launch-while-started/)
and [`launchWhileResumed`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/launch-while-resumed/)
to only execute a coroutine as long as the UI is not stopped. Once stopped, the coroutine is canceled.
In contrast to Android's `launchWhenStarted` this terminates the coroutine instead of suspending it.

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

ReactiveState's [`buildViewModel`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.fragment.app.-fragment/build-view-model/),
[`stateViewModel`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.fragment.app.-fragment/state-view-model/),
[and other](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.fragment.app.-fragment/) extension functions allow creating a `ViewModel` by directly instantiating it.
This results in more natural code and allows passing arguments to the `ViewModel`.
Internally, these helper functions are simple wrappers around `viewModels`, `ViewModelProvider.Factory` and `AbstractSavedStateViewModelFactory`.
They just reduce the amount of boilerplate for common use-cases.

## Installation

Add the package to your `build.gradle`'s `dependencies {}` where `VERSION` should be replaced with the current ReactiveState version:

```groovy
// For Kotlin-only projects
dependencies {
    // ...
    implementation "com.ensody.reactivestate:core:VERSION"
    // ...
}

// For Android projects
dependencies {
    // ...
    implementation "com.ensody.reactivestate:reactivestate:VERSION"
    // ...
}
```

Also, make sure you've integrated the JCenter repo, e.g. in your root `build.gradle`:

```groovy
allprojects {
    repositories {
        // ...
        jcenter()
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
