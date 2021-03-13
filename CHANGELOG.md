# Changelog

## Next release

Breaking changes:

* IMPORTANT: In order to overcome a limitation, the `flowTransformer` argument of `derived`/`coAutoRun`/`CoAutoRunner` must now map over lambda functions and execute them. E.g.: `mapLatest { it() }`. Without the `it()` no value will ever be recomputed!
* The default `flowTransformer` has changed from `{ mapLatest { } }` to `{ conflatedWorker() }`.
* Removed bindings because they turned out to not be useful enough.

Non-breaking changes:

* Added `WhileUsed` for reference-counted singletons that get garbage-collected when all consumers' `CoroutineScope`s end.
* Added `conflatedWorker` and `debouncedWorker`` as simple `flowTransformer`s for the suspend-based `derived`/`coAutoRun`.
* Added `conflatedMap` helper for mapping first and last elements and - whenever possible - intermediate elements.
* Added simple `ErrorEvents` interface and `withErrorReporting(eventNotifier) { ... }` for easy error handling.
* `MutableFlow.tryEmit` now returns a `Boolean`.

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

* `derived` now returns a `StateFlow` instead of a `LiveData`, so you can use `derived` in multi-platform code.
* `LiveDataStore` has been replaced with `StateFlowStore`, so you can write multi-platform code.
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
