# Error handling

## ErrorEvents

This library provides a simple base events interface that is used in several places for error handling called `ErrorEvents`.

Here's the whole implementation

```kotlin
interface ErrorEvents {
    fun onError(error: Throwable)
}
```

Some of the functionality requires that you implement this interface.

## EventNotifier

Your ViewModels and other classes that can launch their own coroutines somehow have to communicate errors to the UI (or higher-level layers in general).

Note: This section only discusses the error-specific aspect of `EventNotifier`.
See [Event handling](event-handling.md) for more general usage of `EventNotifier`.

Imagine this in your business logic or ViewModel:

```kotlin
coroutineScope.launch {
    // someSuspendFun can throw an exception
    someSuspendFun()
}
```

If `someSuspendFun()` throws an exception, how will you let the user know that there is an error?
You need an event queue/dispatcher that is processed by the UI.
That's what `EventNotifier` is.

```kotlin
val eventNotifier = EventNotifier<ErrorEvents>()

fun doSomething() {
    coroutineScope.launch {
        try {
            someSuspendFun()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // This sends the event via eventNotifier
            eventNotifier { onError(e) }
        }
    }
}
```

You can simplify that code by using `withErrorReporting`:

```kotlin
val eventNotifier = EventNotifier<ErrorEvents>()

fun doSomething() {
    coroutineScope.launch {
        withErrorReporting(eventNotifier) {
            someSuspendFun()
        }
    }
}
```

## BaseReactiveState

The multiplatform ViewModel base class `BaseReactiveState` already provides an `eventNotifier` and a `launch` function that catches exceptions:

```kotlin
class MyViewModel(scope: CoroutineScope) : BaseReactiveState<ErrorEvents>(scope) {
    init {
        launch {
            // ...code block...
        }
    }
}
```

This will automatically catch exceptions and trigger `eventNotifier { onError(throwable) }`.

See [Multiplatform ViewModels](multiplatform-viewmodels.md) for more details.
