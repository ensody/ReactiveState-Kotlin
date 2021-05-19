# Event handling

## Events

Events are modeled as simple interfaces where each event is a method:

```kotlin
// The ErrorEvents interface is already part of this library
interface ErrorEvents {
    // The onError event which contains the original exception
    fun onError(error: Throwable)
}

// Now a custom event type
interface FooEvents {
    // the onFoo event which contains a "name" argument
    fun onFoo(name: String)
    // the onOtherFoo event
    fun onOtherFoo()
}

// You can combine multiple events easily via multiple inheritance
interface CombinedEvents : FooEvents, ErrorEvents

// And of course you can also add more events
interface CombinedAndCustomEvents : FooEvents, ErrorEvents {
    fun onCustomEvent()
}
```

The last two examples show why events should be modeled as normal interfaces instead of sealed classes/interfaces.
With normal interfaces you can combine multiple event types very easily (even events defined outside of the current module).

In the next section we'll take a look at how those events can be triggered.

Also see [Error handling](error-handling.md) for details on our `ErrorEvents` interface which is used in several places in this library.

## EventNotifier

The `EventNotifier` class is an event queue on which you can emit events and some other part of your code can collect the events.
The `EventNotifier` is actually a `Channel` wrapped in a `Flow` interface.

Events are buffered until someone collects them.
This is important because you never want to lose events.
In contrast, a `SharedFlow` is lossy - which is often not what you want.

Example how to emit events:

```kotlin
// This EventNotifier can emit any events contained in CombinedEvents
val eventNotifier = EventNotifier<CombinedEvents>()

fun doSomething() {
    // Explicit version
    eventNotifier.tryEmit { onFoo("Slim Shady") }
    // Or the recommended, shorter version
    eventNotifier { onFoo("Slim Shady") }

    eventNotifier { onOtherFoo(e) }

    try {
        // ...
    } catch (e: Throwable) {
        eventNotifier { onError(e) }
    }
}
```

Example how you'd collect events:

```kotlin
// The event listener has to implement the respective events interface(s)
class MyEventListener(scope: CoroutineScope) : CombinedEvents {
    init {
        scope.launch {
            eventNotifier.handleEvents(this@MyEventListener)
        }
    }

    override fun onFoo(name: String) {
        // The onFoo event got triggered.
    }

    override fun onError(error: Throwable) {
        // The onError event got triggered. If MyEventListener is some UI screen
        // you'd probably show an error dialog here.
    }

    // ...
}
```

Note: The [multiplatform ViewModel](multiplatform-viewmodels.md) `BaseReactiveState` already provides a built-in `EventNotifier`.
