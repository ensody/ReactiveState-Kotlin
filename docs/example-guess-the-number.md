# Example: Guess the number

Let's pretend we play the game "guess the correct number" where we can increment a number and submit it to the backend which then tells us whether we guessed correctly.

The following is a short example based on a subset of the principles:

* a multiplatform ViewModel via `BaseReactiveState` (you can alternatively use the `ReactiveState` interface)
* event handling via `BaseReactiveState.eventNotifier`
* automatic error catching via `BaseReactiveState.launch`
* lifecycle aware Android UI via `autoRun` and `by reactiveState` (which observes `BaseReactiveState.eventNotifier`)
* state restoration via `StateFlowStore`

The most important missing bits:

* reactive `StateFlow`s via `derived`
* unit tests

Note: You can alternatively use an Android `ViewModel` and/or combine only the features you prefer to use.

Here's the sample code with a `MainViewModel` which triggers `MainEvents` that are handled by a `MainScreen`:

```kotlin
// ErrorEvents provides just one core event: onError(error: Throwable)
interface MainEvents : ErrorEvents {
    fun onCorrectNumberGuessed(message: String)
}

class MainViewModel(
    scope: CoroutineScope,
    store: StateFlowStore,
) : BaseReactiveState<MainEvents>(scope) {
    // On state restoration, getData() will prefill the StateFlow with
    // the previous value.
    val lastMessage: StateFlow<String?> by store.getData(null)

    // That's the currently guessed number
    val number = MutableStateFlow(0)

    fun increment() {
        number.value += 1
    }

    fun checkNumber() {
        // In contrast to scope.launch, the BaseReactiveState.launch function
        // automatically catches exceptions and triggers ErrorEvents.onError(throwable)
        // using an event queue accessible via BaseReactiveState.eventNotifier.
        launch {
            val successMessage: String? = backend.submitNumber(number.value)
            // Persist the last message for state restoration
            lastMessage.value = successMessage
            if (successMessage != null) {
                // Yay, we have guessed correctly! We should show a "success" dialog.
                // Since the dialog must be shown exactly once (even on UI recreation
                // or state restoration), we communicate the result via an event.
                // The code block passed to eventNotifier will be executed on the
                // MainScreen/Fragment in the >= STARTED state (it's lifecycle aware).
                eventNotifier {
                    onCorrectNumberGuessed(successMessage)
                }
            }
        }
    }
}

// Now some Android-specific code for the main screen (hopefully with Jetpack Compose
// the screen code will also become multiplatform someday)

// The Fragment has to implement its ViewModel's events interface (MainEvents).
class MainScreen : Fragment(), MainEvents {
    // Attaches a multiplatform ViewModel (a ReactiveState instance) to the fragment.
    // Within the `by reactiveState` block you have access to scope and stateFlowStore
    // which are taken from an internally created Android ViewModel that hosts the
    // ReactiveState instance.
    // The stateFlowStore is backed by a Android's SavedStateHandle (the official way to
    // deal with saved instance state at the ViewModel layer).
    // Finally, `by beactiveState` automatically processes MainViewModel.eventNotifier
    // within the >= STARTED state, as promised in the comment above.
    private val viewModel by reactiveState { MainViewModel(scope, stateFlowStore) }

    // Let's use Android view bindings
    private lateinit var binding: MainViewBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Let's imagine we have a button to increment the current number
        binding.incrementButton.setOnClickListener {
            viewModel.increment()
        }
        // And another button to check if the current number is correct
        binding.checkNumberButton.setOnClickListener {
            viewModel.checkNumber()
        }

        // With autoRun you can keep the UI in sync with one or more StateFlows.
        // The get() call retrieves the StateFlow.value and tells autoRun to re-execute
        // the code block whenever the accessed StateFlow is changed.
        // Here we want the info text to always show the current number.
        autoRun {
            binding.infoTextView.text = "Your current guess: ${get(viewModel.number)}"
        }

        // Once we've found the correct number we want to disable the
        // "increment" and "check number" buttons because the game is over.
        // Also, if we're currently doing a request we want to disable the buttons, too.
        autoRun {
            // Here we're watching multiple StateFlows at the same time.
            // BaseReactiveState.loading is a StateFlow<Int> tracking the number of
            // currently running coroutines that were started via BaseReactiveState.launch.
            // This is just the simplest possible example.
            // Instead of using `loading` you can also distinguish between different
            // loading states if you want.
            val buttonsEnabled =
                get(viewModel.lastMessage) == null && get(viewModel.loading) == 0
            binding.incrementButton.isEnabled = buttonsEnabled
            binding.checkNumberButton.isEnabled = buttonsEnabled
        }

        // You'd usually also observe viewModel.loading (or other loading states) to
        // show a loading indicator, but let's keep this example small...
    }

    override fun onCorrectNumberGuessed(message: String) {
        // Show a nice dialog, congratulating the user
    }

    override fun onError(error: Throwable) {
        // This event is triggered if any exception is uncaught in the ViewModel.
        // Usually you'd show an error dialog here.
    }
}
```

All of this code is lifecycle aware, only executing in the `>= STARTED` state.

You don't need to plaster all your code with copy-pasted `try-catch` logic. Most of the time you can deal with errors in `onError()`.
