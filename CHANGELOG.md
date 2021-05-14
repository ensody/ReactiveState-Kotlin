# Changelog

## Next release

This release adds support for Kotlin Multiplatform.

Breaking changes:

* The modules have been restructured and renamed:
  * dependency-versions-bom => reactivestate-bom
  * core => reactivestate
  * core-test => reactivestate-test
* `CoroutineTest` has become independent of JUnit.
* `CoroutineTestRule` is now a simple class that you can either derive from or add as an attribute.

Known limitations which will be solved with next releases:

* On non-JVM platforms, `dispatchers.io` currently equals `Dispatchers.Default`.
* This primarily affects `MutableValueFlow`: Internally, all uses of the JVM-only `synchronized` have been replaced with a spinlock `Mutex` since they were only utilized for very tiny blocks of code which normally don't even have any parallel access. Be careful about doing too large computations in combination with highly concurrent updates via `replaceLocked`, though.

## 3.9.0

* Added `@ExperimentalReactiveStateApi` annotation to mark experimental APIs.
* Explicitly marked `SuspendMutableValueFlow` as experimental and changed its constructor to take the default value instead of a delegating `MutableValueFlow`.
* Turned `SuspendMutableValueFlow` into an interface and constructor/factory function.

## 3.8.3

* Fixed argument inconsistency in `SuspendMutableValueFlow.replace`.

## 3.8.2

* Fixed state restoration with `SavedStateHandleStore`.

## 3.8.1

* Fixed support for nullable values in `SavedStateHandleStore`.

## 3.8.0

* Added `by propertyName` and `by lazyProperty` helpers simplifying `ReadOnlyProperty`.

## 3.7.0

* `MutableValueFlow`'s constructor now optionally takes a `setter` lambda function which is executed before emitting a new value. This allows observing / reacting to changes without needing a `CoroutineScope`.
* Added `val Fragment/Activity.savedInstanceState` helper which gives you access to a `StateFlowStore` where you can put saved instance state.
* Added `by stateFlowViewModel` helpers which work like `by stateViewModel` but use a `StateFlowStore`.
* Added `by Fragment/Activity.savedInstanceState(default)` and `by StateFlowStore.getData(default)` and extension function which automatically uses a key based on the property name.
* `SavedStateHandleStore` now provides an alternative constructor with doesn't require a `CoroutineScope` and only has one-way `MutableValueFlow -> LiveData` sync (which covers the 99% use-case).

## 3.6.0

* Added `SuspendMutableValueFlow` for values that must be mutated via a suspend fun.

## 3.5.0

* The lambda function for `WhileUsed` now receives a `WhileUsedReferenceToken` which has a lazy `scope` attribute in case you need a `CoroutineScope` with the same lifetime as your `WhileUsed` value.
* Fixed `withLoading` behavior of first `derived` calculation.

## 3.4.0

* Added `buildOnViewModel` which allows creating arbitrary objects living on an internally-created wrapper ViewModel.
* Added `MutableStateFlow.replace` and `MutableValueFlow.replaceLocked` helper functions for simplifying e.g. data class `copy()` based mutation.

## 3.3.0

* Added `NamespacedStateFlowStore`.
* Added `ReducingStateFlow`.

## 3.2.1

* Moved from JCenter to Maven Central ([JCenter is shutting down](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/)). Make sure your `repositories` block looks like this:

```groovy
repositories {
    google()
    mavenCentral()
    // ...
}
```

## 3.2.0

* `CoroutineTest` now implements `AttachedDisposables` and disposes at the end of `runBlockingTest`.
* `CoroutineTest` provides `collectFlow` to easily collect a `derived` with `WhileSubscribed()` in background.

## 3.1.1

* Fixed `withErrorReporting` to really accept a suspension function.

## 3.1.0

* Added `withErrorReporting` to handle suspension functions and optional `onError`.
* Added `EventNotifier.handleEvents` helpers for Kotlin and Android. The Android version takes a `LifecycleOwner`.
* Added `EventNotifierTest`.

## 3.0.0

Breaking changes:

* IMPORTANT: In order to overcome a limitation, the `flowTransformer` argument of `derived`/`coAutoRun`/`CoAutoRunner` must now map over lambda functions and execute them. E.g.: `mapLatest { it() }`. Without the `it()` no value will ever be recomputed!
* The default `flowTransformer` has changed from `{ mapLatest { } }` to `{ conflatedWorker() }`. There is also `latestWorker` if you want the old behavior.
* The possible arguments to `derived` were changed a little bit in order to improve compatibility with `WhileUsed`. Either you must remove the `started: SharingStarted` argument or additionally pass an `initial` value.
  * If you leave out the `started` argument `derived` behaves like before when passing `Eagerly`: the value is computed immediately and synchronously and you can't call `suspend` functions within the observer.
  * Otherwise, you have to pass an `initial` value. In this case, `derived` is asynchronous and you can call `suspend` functions within the observer.
  * The default value for `started` is now `Eagerly` because that has better safety guarantees. So, in most usages the whole argument can now be removed (except where you really need `WhileSubscribed()`, for example).
* Removed bindings because they turned out to not be useful enough.
* `Resolver.track()` now returns the `AutoRunnerObservable` instead of the `underlyingObservable`.

Non-breaking changes:

* Added `WhileUsed` for reference-counted singletons that get garbage-collected when all consumers' `CoroutineScope`s end.
* Added `conflatedWorker`, `latestWorker` and `debouncedWorker` as simple `flowTransformer`s for the suspend-based `derived`/`coAutoRun`.
* Added `conflatedMap` helper for mapping first and last elements and - whenever possible - intermediate elements.
* Added simple `ErrorEvents` interface and `withErrorReporting(eventNotifier) { ... }` for easy error handling.
* `MutableFlow.tryEmit` now returns a `Boolean`.

Legal change:

* Switched license to Apache 2.0.

## 2.0.4

* Upgraded dependencies (coroutines 1.4.3, androidx.lifecycle 2.3.0, fragment-ktx 1.3.1)

## 2.0.2

* Fixed BOM (reactivestate was missing).

## 2.0.0

Breaking changes:

* Moved all Android-related code to the `com.ensody.reactivestate.android` package to avoid method resolution ambiguity.
* Removed `CoroutineScopeOwner` (replaced by `CoroutineLauncher`).

Non-breaking changes:

* Added `CoroutineLauncher` interface to allow overriding how `autoRun`/`derived` launch their coroutines (e.g. to include custom error handling or a loading state).
* Added `coAutoRun` and `CoAutoRunner` and `derived` variants which take suspension functions and use `mapLatest`.

## 1.1.0

* `StateFlowStore.getData()` now returns a `MutableValueFlow`

## 1.0.0

* `derived` now takes a mandatory `started: SharingStarted` argument - similar to `stateIn`/`shareIn` (`WhileSubscribed()`, `Lazily`, `Eagerly`, etc.)

## 0.15.4

* Fixed `CoroutineTest`.

## 0.15.3

* Added support for using `CoroutineTest` by delegation (preventing multiple inheritance situations).

## 0.15.2

* Added `buildViewModel` and `stateViewModel` extension functions for `Activity`.

## 0.15.0

* `derived` supports an optional `lazy = true` argument to observe lazily.
* Added a global `dispatchers` API for replacing `Dispatchers` (`Main`, `IO`, etc.) in a way that allows switching to `TestCoroutineDispatcher` in unit tests.
* Added coroutine unit test helpers in the `com.ensody.reactivestate:core-test` module:
  * `CoroutineTest` base class for tests that use coroutines. This sets up  `MainScope`, `dispatchers.io`, etc. to use `TestCoroutineDispatcher`.
  * `CoroutineTestRule` a test rule for setting up coroutines.
  * `CoroutineTestRuleOwner` a helper interface in case you can't use `CoroutineTest`, but still want minimal boilerplate.
* Removed `launchWhileStarted` and `launchWhileResumed`.
* Added `dependency-versions-bom` platform project. You can now include the versions of all modules like this:

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

## 0.14.0

* Fixed `MutableValueFlow.value` assignment to have `distinctUntilChanged` behavior. This should provide the best of both worlds:
  * `emit`/`tryEmit`/`update` always emit
  * `.value` behaves exactly like with `MutableStateFlow`

## 0.13.0

After a long period of tuning the API and use at several companies this release introduces the hopefully last set of major breaking changes.

This is the final migration to `Flow`-based APIs like `StateFlow`/`SharedFlow`/`ValueFlow` and removal of obsolete APIs.

* `autoRun` now auto-disposes in `Activity.onDestroy`/`Fragment.onDestroyView`, so usually it should be launched in `Activity.onCreate()`/`Fragment.onCreateView()` (previously `onStart()`). It still automatically observes only between `onStart()`/`onStop()`.
* Removed `MutableLiveDataNonNull` and other non-null LiveData helpers. Use `MutableStateFlow` and `MutableValueFlow` instead.
* Added `MutableValueFlow` which implements `MutableStateFlow`, but doesn't have `distinctUntilChanged` behavior and offers an in-place `update { it.attr = ... }` method. This makes it safer and easier to use with mutable values.
* Removed `DerivedLiveData`. Use `DerivedStateFlow`/`derived` instead.
* Replaced `workQueue` with the much simpler `EventNotifier` which allows sending one-time events to the UI.
* Replaced `Scoped` with a simple `CoroutineScopeOwner` interface.
* Upgraded to Kotlin 1.4.10.

## 0.12.0

Breaking changes (migration to `StateFlow`):

* `derived` now returns a `StateFlow` instead of a `LiveData`, so you can use `derived` in multiplatform code.
* `LiveDataStore` has been replaced with `StateFlowStore`, so you can write multiplatform code.
* `InMemoryStore` has been replaced with `InMemoryStateFlowStore`.
* `SavedStateHandleStore` now implements `StateFlowStore` and requires a `CoroutineScope` in addition to `SavedStateHandle`.
* `State` has been renamed to `Scoped` (more descriptive).

Other changes:

* Added `StateFlow`-based API for bindings. The `LiveData`-based API is still available.

## 0.11.4

* Switched to `api` instead of `implementation` for most dependencies.

## 0.11.3

* Fixed edge case with `autoRun` on `LiveData` incorrectly ignoring the first notification.

## 0.11.2

* Fixed release packaging of Android reactivestate package.

## 0.11

ATTENTION: There's a breaking change to support `StateFlow`.

* Added experimental `StateFlow` support to `autoRun` (`StateFlow` was added in `kotlinx-coroutines-core` 1.3.6).
* Breaking change: Removed `CoroutineContext.autoRun` and `suspend fun autoRun` because proper `StateFlow` support requires access to a `CoroutineScope`.

## 0.10

* Added `thisWorkQueue` helper for passing an arg via `this`.
* `argWorkQueue` now supports suspension functions.
* Added `AttachedDisposables` interface for objects that can clean up other disposables.
* `AutoRunner` now implements `AttachedDisposables`.
* Added `OnDispose { ... }` class for triggering a function when its `dispose()` method is called.
* Added `onDestroyView { ... }`, `onDestroy`, `onCreate`, `onCreateView` and their `...Once` variants.
* Added `validUntil` for properties that are only valid during a lifecycle subset.
* Further documentation improvements.

## 0.9.1

This release introduces no code changes.

* Fixed release publication on jcenter.
* Minor documentation improvements.

## 0.9

ATTENTION: Due to the core & reactivestate module split you have to also add the core module to your dependencies as described in the [installation instructions](https://ensody.github.io/ReactiveState-Kotlin/#installation).

* This release splits the `reactivestate` module into a Kotlin module (`core`) and an Android module (`reactivestate`). The change is backwards-compatible unless you've accessed `BaseAutoRunner` (very unlikely).
* Added `State` base class for separating business logic from `ViewModel`, making it easier to use in normal unit tests.
* Added `argWorkQueue`, arg-based `consume` and `consumeConflated` helpers for ViewModel -> UI event/notification use-case.
* Fixed one-level recursion when observing `LiveData`.
* Documentation improvements.

## 0.8

ATTENTION: This release comes with a few breaking changes.

* Bindings don't have value converters, anymore. Usually you need to store the raw field value and a separate conversion (if possible without errors), anyway. Use `autoRun` or `derived` to convert values.
* Added bindings for `CompoundButton` (replacing `CheckBox` bindings).
* `AutoRunner`'s and `autoRun`'s `observer` callback now receives the `Resolver` via `this` instead of as an argument (more consistent and compact code). You can write `get(livedata)` to retrieve a `LiveData` value.
* `AutoRunner`'s and `autoRun`'s `onChange` callback now receives the `AutoRunner` as its first argument.
* Improved `AutoRunner`'s null handling of `LiveData`.
* Added `onResume`, `onResumeOnce`, `onPause`, `onPauseOnce`, `launchWhileResumed` lifecycle observers.
* Added `Disposable.disposeOnCompletionOf(coroutineContext)` extension methods.
* Added `WorkQueue` and helpers like `conflatedWorkQueue` for simpler communication between UI and ViewModel.
* Added unit tests.
* Added simple documentation with API reference.

## 0.5

Initial release.
