# Correct lifecycle handling

```kotlin
interface MainEvents : ErrorEvents {
    fun showMessage(message: String)
}

// You can create a multiplatform ViewModel by deriving from
// BaseReactiveState instead. More details below.
class MainViewModel : ViewModel() {
    // This queue can be used to send events to the MainEvents in the STARTED
    // lifecycle state. Instead of boilerplaty event sealed classes we use a
    // simple MainEvents interface with methods.
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

## Automatic cleanups based on lifecycle state

Especially on Android it's very easy to shoot yourself in the foot and e.g. have a closure that keeps a reference to a destroyed `Fragment` or mistakenly execute code on a destroyed UI.

ReactiveState provides a `Disposable` interface and most objects auto-dispose/terminate when a `CoroutineScope` or Android `Lifecycle` ends.
You can also use `disposable.disposeOnCompletionOf` to auto-dispose your disposables.
For more complex use-cases you can use `DisposableGroup` to combine (add/remove) multiple disposables into a single `Disposable` object.

With extension functions like `LifecycleOwner.onResume` or `LifecycleOwner.onStopOnce` you can easily add long-running or one-time observers to a `Lifecycle`.
These are the building blocks for your own lifecycle-aware components which can automatically clean up after themselves like `LifecycleOwner.autoRun` does.

Finally, with `validUntil()` you can define properties that only exist during a certain lifecycle subset and are dereference their value outside of that lifecycle subset.
This can get rid of the ugly [boilerplate](https://developer.android.com/topic/libraries/view-binding#fragments) when working with view bindings, for example.
