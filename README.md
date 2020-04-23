# ReactiveState for Kotlin and Android

[ ![Download](https://api.bintray.com/packages/ensody/maven/com.ensody.reactivestate%3Areactivestate/images/download.svg) ](https://bintray.com/ensody/maven/com.ensody.reactivestate%3Areactivestate/_latestVersion)

An easy to understand reactive state management solution for Kotlin and Android.

This library is based on [reactive_state](https://github.com/ensody/reactive_state) for Flutter and adapted to Kotlin and Android patterns and problems.

With `autoRun { ... }` you can observe and re-execute a function whenever any of the `LiveData` instances accessed by the function is modified.
This is useful e.g. for keeping the UI in sync with your ViewModel.
Depending on the context in which `autoRun` is executed, this observer is automatically tied to a `CoroutineScope` (e.g. the `ViewModel`'s `viewModelScope`) or in case of a `Fragment`/`Activity` to the `onStart()`/`onStop()` lifecycle.

With `bind()` and `bindTwoWay()` you can easily create one-way or two-way bindings between `LiveData` and your views.
These bindings are automatically tied to the `onStart()`/`onStop()` lifecycle of your `Fragment`/`Activity` in order to *prevent accidental memory leaks*.
This means you have to create your bindings in `onStart()`.

## Installation

Add the package to your `build.gradle`'s `dependencies {}` where `VERSION` should be replaced with the current ReactiveState version:

```groovy
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
    val count = savedStateHandle.getLiveDataNonNull("count", 0)

    // These store form data
    val username = savedStateHandle.getLiveDataNonNull("username", "")
    val password = savedStateHandle.getLiveDataNonNull("password", "")
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
    private val state by stateViewModel { MainViewModel(it) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = MainFragmentBinding.inflate(inflater, container, false).root

    // All bindings and autoRun calls get disposed during onStop(), so
    // we always re-create them in onStart()
    override fun onStart() {
        super.onStart()
        val binding = MainFragmentBinding.bind(requireView())

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
