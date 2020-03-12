# ReactiveState for Android

[ ![Download](https://api.bintray.com/packages/ensody/maven/com.ensody.reactivestate%3Areactivestate/images/download.svg) ](https://bintray.com/ensody/maven/com.ensody.reactivestate%3Areactivestate/_latestVersion)

An easy to understand reactive state management solution for Android.

This is based on [reactive_state](https://github.com/ensody/reactive_state) for Flutter.

State is held in one or multiple instances of `LiveData`.
These are standard Android classes that are widely in use and can be converted to/from `Flow`.
This library also provides non-nullable `LiveData` variants.

With `bind()` and `bindTwoWay()` you can easily create one-way or two-way bindings between `LiveData` and your views.
These bindings are automatically tied to the `onStart()`/`onStop()` lifecycle of your `Fragment`/`Activity` in order to prevent memory leaks.
This means you have to re-create your bindings in `onStart()`.

With `autoRun { ... }` you can observe and re-execute a block of code whenever any of the `LiveData` instances accessed by the block is modified.
Depending on the context in which `autoRun` is executed, this observer is automatically tied to a `CoroutineScope` (e.g. the `ViewModel`'s `viewModelScope`) or in case of a `Fragment`/`Activity` to the `onStart()`/`onStop()` lifecycle.

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
    val username = savedStateHandle.getLiveDataNonNull("username", "")
    val generated = MutableLiveDataNonNull("")
    val derived = derived { get ->
        "${get(username)} ${get(count)} ${get(generated)}"
    }

    init {
        autoRun { get ->
            generated.value = "${get(username)} ${get(count)}"
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = MainFragmentBinding.inflate(inflater, container, false).root

    // All bindings and autoRun calls get disposed during onStop(), so
    // we re-create them in onStart()
    override fun onStart() {
        super.onStart()
        val binding = MainFragmentBinding.bind(requireView())

        bindTwoWay(model.username, binding.message)
        bind(binding.resultMessage, model.derived)
        autoRun { get ->
            val hasUsername = get(model.username).isNotEmpty()
            binding.resultMessage.visibility =
                if (hasUsername) View.VISIBLE else View.GONE
            binding.increment.isEnabled = hasUsername
        }
        binding.increment.setOnClickListener {
            model.increment()
        }
    }
}
```
