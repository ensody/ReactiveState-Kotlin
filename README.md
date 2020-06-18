# ReactiveState for Kotlin and Android

[ ![Download](https://api.bintray.com/packages/ensody/maven/com.ensody.reactivestate%3Areactivestate/images/download.svg) ](https://bintray.com/ensody/maven/com.ensody.reactivestate%3Areactivestate/_latestVersion)

An easy to understand reactive state management solution for Kotlin and Android.

This library is split into two separate modules for Kotlin ([`core`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/)) and Android ([`reactivestate`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/)).

## Use-cases

### Keeping UI in sync with state

Note: While this is an Android example, `autoRun` (`core`) also has experimental support for `StateFlow` which is multi-platform compatible.

```kotlin
class MainViewModel : ViewModel() {
    // You can also use the normal MutableLiveData, but then you'll have to deal with null.
    val name = MutableLiveDataNonNull("")
    val counter = MutableLiveDataNonNull(0)

    fun increment() {
        counter.value += 1
    }
}

class MainFragment : Fragment() {
    private val model by viewModels<MainViewModel>()

    override fun onStart() {
        // ...
        // val nameInputField = ...
        // val incrementButton = ...

        bindTwoWay(model.name, nameInputField)

        autoRun {
            // get() returns the LiveData.value and tells autoRun to re-execute
            // this code block whenever model.name or model.counter is changed.
            // Result: isEnabled changes while you type.
            incrementButton.isEnabled = get(model.name).isNotEmpty() && get(model.counter) < 100
        }

        incrementButton.setOnClickListener {
            model.increment()
        }
    }
}
```

With [`autoRun`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/auto-run/) (available on `LifecycleOwner`, `ViewModel`, [`Scoped`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/com.ensody.reactivestate/-scoped/), `CoroutineScope`, etc.)
you can observe and re-execute a function whenever any of the `StateFlow` or `LiveData` instances accessed by that function are modified.
On Android you can use this to keeping the UI in sync with your ViewModel. Of course, you can also keep non-UI state in sync.
Depending on the context in which `autoRun` is executed, this observer is automatically tied to a `CoroutineScope` (e.g. the `ViewModel`'s `viewModelScope`) or in case of a `Fragment`/`Activity` to the `onStart()`/`onStop()` lifecycle.

With [`bind`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/bind/)
and [`bindTwoWay`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-lifecycle-owner/bind-two-way/) you can easily create one-way or two-way bindings between `LiveData` and your views.
These bindings are automatically tied to the `onStart()`/`onStop()` lifecycle of your `Fragment`/`Activity` in order to prevent accidental memory leaks and unnecessary resource consumption.
This means you have to create your bindings and `AutoRunner`s in `onStart()`.

Note that `autoRun` and `bind` can be extended to support observables other than `StateFlow` and `LiveData`.

### Running operations outside of the UI lifecycle

```kotlin
class MainViewModel : ViewModel() {
    // This queue can be used to throttle actions using 200ms windows
    val queue = conflatedWorkQueue(200)
    // This queue can be used to execute a response on the MainFragment (latest
    // instance passed in as `this`).
    // Note: In most cases you'll want to store the result in LiveData.
    // This is only meant for actual events/navigation instead of state.
    // XXX: You'll probably want to use an interface instead of MainFragment.
    val responses = thisWorkQueue<MainFragment>()

    fun someAction() {
        queue.launch {
            val result = api.requestSomeAction()

            // Switch back to MainFragment (the latest visible instance).
            responses.launch {
                // If the screen got rotated in the meantime, `this` would point
                // to the new MainFragment instance instead of the destroyed one
                // that did the initial `someAction` call above.
                showPopUp(result.someMessage)
                // Instead of showing a pop-up you could also navigate to some other Fragment.
                // findNavController()...
            }
        }
    }
}

class MainFragment : Fragment() {
    private val model by viewModels<MainViewModel>()

    init {
        lifecycleScope.launchWhenStarted {
            // Execute responses, passing MainFragment to each lambda
            model.responses.consume(this@MainFragment, this)
            // Alternatively: model.responses.conflatedConsume(this@MainFragment, this, 200)
        }
    }

    // ...

    override fun onStart() {
        // ...
        // val button = ...

        button.setOnClickListener {
            model.someAction()
        }
    }

    fun showPopUp(message: String) {
        // ...
    }
}
```

On Android, managing operations independently of the UI lifecycle (e.g. button click -> request -> UI rotated -> response -> UI update/navigation) is made unnecessarily difficult because Android can destroy your UI in the middle of an operation.
To work around this, you'll usually launch a coroutine in `ViewModel.viewModelScope` and/or use a `Channel` to communicate between the `ViewModel` and the UI.

In order to simplify this pattern, ReactiveState provides [`WorkQueue`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/com.ensody.reactivestate/-work-queue/) and helpers like
[conflatedWorkQueue](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-view-model/conflated-work-queue/)
(available for `LifecycleOwner`, `ViewModel`, `CoroutineScope`, [`Scoped`](https://ensody.github.io/ReactiveState-Kotlin/reference/core/com.ensody.reactivestate/-scoped/), etc.).
A `WorkQueue` is just a `Channel` and a `Flow` consuming that channel.
You get full access to the `Flow` to configure it however you want (`debounce()`, `conflate()`, `mapLatest()`, etc.).

Usually, you'd use a `WorkQueue` to throttle UI events and execute event handlers in a `ViewModel`.
Also, you can build a request-response event pipeline between the UI and the `ViewModel` with helpers like [`thisWorkQueue`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-view-model/this-work-queue/) (see example above).
Of course, you can also throttle UI events with [FlowBinding](https://github.com/ReactiveCircus/FlowBinding) and only use a `WorkQueue` to serialize the event execution or handle request-response-style messaging.

While you can process any type of event in a `WorkQueue`, most helper methods are built around processing lambda functions, so you don't need to write boilerplate (defining event classes and dispatching on them in huge `when` statements):

### Automatic cleanups and lifetime/lifecycle management

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

Since the start/stop (and resume/pause) lifecycle matches most closely to a `LifecycleOwner`'s visibility/usage, most of ReactiveState's Android extension functions require launching in `onStart()` and then the code auto-terminates/disposes when the lifecycle triggers an `onStop()`.
Normally, you only want to update the UI when it's visible (in the foreground), anyway. For any other background processing you can still use e.g. `autoRun` on `ViewModel` or `lifecycleScope` and have the UI re-sync in `onStart()` once it becomes visible again.
The `onCreateView()`/`onDestroyView()` cycle (via `viewLifecycleOwner`) isn't used because when the `Fragment`'s `Activity` moves to the back stack you only receive an `onStop()` without `onDestroyView()`.
Also, an `Activity` only receives `onStop()`. In order to make the API consistent and prevent surprises we just use `onStart()`/`onStop()`.

Finally, with `validUntil()` you can define properties that only exist during a certain lifecycle subset and are dereference their value outside of that lifecycle subset.
This can get rid of the ugly [boilerplate](https://developer.android.com/topic/libraries/view-binding#fragments) when working with view bindings, for example.

### Non-nullable LiveData

```kotlin
val livedata = MutableLiveData(1)
val fixed = livedata.fixValueType()
val nonnull = MutableLiveDataNonNull(1)
nullable.value // => Type: Int?
fixed.value    // => Type: Int
nonnull.value  // => Type: Int

autoRun {
    get(livedata) // => Type: Int?
    get(fixed)    // => Type: Int
    get(nonnull)  // => Type: Int
}

```

Android's `LiveData<T>.value` is unfortunately nullable (type `T?`).
This leads to unnecessary null checks and complicates code even if `value` is guaranteed to never be `null`.

ReactiveState provides a [`MutableLiveDataNonNull<T>`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/-mutable-live-data-non-null/)
which fixes the type of `value` to be `T` instead of `T?`.
In contrast to `MutableLiveData`, the constructor requires an initial value.
When using `MutableLiveDataNonNull` with `autoRun`, the [`get`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/get/)
function returns a non-null value (`T`) while `get()` on a normal `LiveData` returns a nullable value (`T?`).

With [`LiveData.fixValueType`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.lifecycle.-live-data/)
you can convert an existing nullable `LiveData` into a non-nullable one.

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
    private val model by viewModel { MainViewModel(SomeDependency()) }
    private val model2 by stateViewModel { handle -> StateViewModel(handle, SomeDependency()) }
}
```

ReactiveState's [`viewModel`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.fragment.app.-fragment/view-model/),
[`stateViewModel`](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.fragment.app.-fragment/state-view-model/),
[and other](https://ensody.github.io/ReactiveState-Kotlin/reference/reactivestate/com.ensody.reactivestate/androidx.fragment.app.-fragment/) extension functions allow creating a `ViewModel` by directly instantiating it.
This results in more natural code and allows passing arguments to the `ViewModel`.
Internally, these helper functions are just wrappers around `viewModels`, `ViewModelProvider.Factory` and `AbstractSavedStateViewModelFactory`.
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
// We separate the actual state object from ViewModel because this
// makes most of our code platform-independent (e.g. works on Android and iOS)
// and allows writing unit tests without Robolectric.
class MainViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val state = MainState(viewModelScope, SavedStateHandleStore(viewModelScope, savedStateHandle))
}

class MainState(scope: CoroutineScope, store: LiveDataStore) : Scoped(scope) {
    val count = store.getLiveData("count", 0)

    // These store form data
    val username = store.getLiveData("username", "")
    val password = store.getLiveData("password", "")
    val usernameError = MutableLiveDataNonNull("")
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
        // cases (e.g. if you need to set multiple LiveData values or
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
    private val model by stateViewModel { MainViewModel(it) }
    private val state get() = model.state
    private var binding by validUntil<MainFragmentBinding>(::onDestroyView)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    // All bindings and autoRun calls get disposed during onStop(), so
    // we always re-create them in onStart()
    override fun onStart() {
        super.onStart()

        // Two-way bindings (String/TextView or Bool/CompoundButton)
        bindTwoWay(state.username, binding.username)
        bindTwoWay(state.password, binding.password)

        // One-way bindings (also possible in the other direction)
        bind(binding.usernameError, state.usernameError)
        bind(binding.passwordError, state.passwordError)

        // One-way binding using more flexible autoRun callback style.
        bind(binding.count) {
            // NOTE: You'll probably want to localize this string.
            "${get(state.count)}"
        }

        // Even more complicated cases can use autoRun directly.
        autoRun {
            // Only enable submit button if form is valid
            binding.submitButton.isEnabled = get(state.isFormValid)
        }

        autoRun {
            val invalid = get(state.usernameError).isNotEmpty()
            // Show username error TextView only when there is an error
            binding.usernameError.visibility =
                if (invalid) View.VISIBLE else View.GONE
        }

        binding.increment.setOnClickListener {
            state.increment()
        }
    }
}
```

## See also

This library is based on [reactive_state](https://github.com/ensody/reactive_state) for Flutter and adapted to Kotlin and Android patterns.
