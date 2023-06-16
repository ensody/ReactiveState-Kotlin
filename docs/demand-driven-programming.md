# Demand-driven programming

Demand-driven means that expensive resources are automatically (on-demand) only allocated as long as they're needed and then freed when not needed.
This also includes computations which should only happen when they're needed.

For example, `by lazy` is not demand-driven because it only allocates resources on first use, but never deallocates.

However, `stateIn` together with `WhileSubscribed` allows creating a demand-driven `StateFlow` that might e.g. communicate with a backend via a WebSocket (let's say a news ticker).

Normally you want to only keep the connection open as long as there is some UI that displays the data.
Once the user logs out or switches to some other screen or just locks his screen you want to close the connection.

The point of being demand-driven is to make this automatic, so your code becomes 100% robust and simple, no matter in which way you change your UI.
You should never have to plaster your code with explicit `open()` and `close()` logic everywhere because in practice this "imperative" logic leads to bugs.
Imagine the a new requirement where you want to show the news widget in a few more screens, so now the connection needs to be kept open across some screens, but on others it can be closed.

The way to model this problem is that some screens "demand" the news ticker.
In other words, they have a dependency on the news ticker.

## StateFlow and SharedFlow

The coroutines library only provides `stateIn` and `shareIn` functions which take a `CoroutineScope`.
This can be quite annoying if you want to e.g. create a function that returns a `StateFlow` because
`stateIn` keeps a coroutine running in the background as long as the provided `CoroutineScope` exists.
Also, very often in reusable/library code you don't want to pass scopes around through your whole codebase because
that's just ugly and feels unnecessary. Moreover, you might not even have a proper `CoroutineScope` available because
your computed `StateFlow` shouldn't bind to a single ViewModel's scope, but rather be shared maybe even by multiple
ViewModels, but still not consume resources when nobody is using that `StateFlow`.
Ideally you wouldn't have to deal with `CoroutineScope`.

This library provides `stateOnDemand` and `shareOnDemand` and `sharedFlow` which only launch an internal coroutine
while someone is subscribed, but can safely get garbage collected when nobody is subscribed, anymore.

For example, a database `query()` function might want to return the current result but also allow observing for updates.
With `stateOnDemand` you can create such a result:

```
suspend fun query(sql: String): StateFlow<QueryResult> =
    callbackFlow {
        // Pseudo-API for getting notified whenever a SQL query's results get updated
        val observer = QueryObserver(sql) { newResult ->
            send(newResult)
        }
        observer.start()
        awaitClose { observer.dispose() }
    }.stateOnDemand { previous: Wrapped<QueryResult>? ->
        // stateOnDemand takes a getter function which defines StateFlow.value when nobody collects.
        // The previous value is also passed here, wrapped in a Wrapped() instance (which can be null if this is the
        // first value access).
        previous?.value
            ?: executeQueryAndGetResult(sql)
    }

/** Executes a non-observable query. Used internally to fill the initial StateFlow.value. */
private suspend fun executeQueryAndGetResult(sql: String): QueryResult = ...
```

Here we've turned a simple `Flow` (via `callbackFlow`) into a `StateFlow` that is safe for returning from a function
and we didn't need any `CoroutineScope`.

## News ticker example

Imagine you want to show the breaking news only, but the backend only provides a list of all latest news.
We have to filter the breaking news from that list, but all of that should only happen while the screen is visible.
If the user locks the screen or switches the app the connection should stop.

```kotlin
class NewsTicker(scope: CoroutineScope) {
    val latestNews: StateFlow<List<News>> = channelFlow {
        // connect to backend, watch for changes and send() latest news,
        // and close the connection when the flow is cancelled
    }.stateIn(initial = emptyList(), started = WhileSubscribed())
}

// A demand-driven singleton which you can put into your DI
val newsTicker = WhileUsed {
    // it.scope is a MainScope that exists only as long someone depends on newsTicker
    // and is cancelled once it's not needed anymore.
    NewsTicker(it.scope)
}

val breakingNews: StateFlow<List<News>> =
    derived(initial = emptyList(), started = WhileSubscribed()) {
        get(get(newsTicker).latestNews)
            .filter { it.isBreakingNews }
    }

class NewsScreen : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // autoRun only collects in >= STARTED state.
        // When the user e.g. locks the screen or switches to some other app,
        // it cancels collecting because the state is < STARTED.
        autoRun {
            updateScreen(get(breakingNews))
        }
    }

    fun updateScreen(news: List<News>) {
        // ...
    }
}
```

As you can see, some of this depends on Kotlin's [`WhileSubscribed()`](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-sharing-started/-while-subscribed.html).

`WhileUsed` allows you to create an on-demand computed singleton that gets disposed as soon as nobody is using it, anymore.
Here we're applying it to only create the `NewsTicker` object as long as it's needed.

Internally, `WhileUsed` is tracking the number of reference, so once that "reference count" goes back to `count == 0` it can destroy its value and `CoroutineScope`.

As an alternative to the `autoRun`/`derived` based tracking, you can also use a `CoroutineScope` to track the dependency lifetime:

```kotlin
// This holds the reference until coroutineScope is cancelled.
// Note: it's lowercase newsTicker, so we're calling the WhileUsed object here.
val newsTickerInstance = newsTicker(coroutineScope)
```

If even this doesn't work for you, there are other alternatives like manual reference count tracking or passing a `DisposableGroup`.
