# Changelog

## 5.13.0

* Added `OnReactiveStateAttachedTo.onReactiveStateAttachedTo(parent)`, so the ViewModel can finish its initialization and do additional checks against the parent (which can be the UI or a parent ViewModel).

## 5.12.0

* Fixed `by childReactiveState` to call `onReactiveStateAttached(child)` (the arguments were inverted).

## 5.11.0

* Added `MutableStateFlow<T>.collectAsMutableState()` to create a two-way binding.
* Added `MutableState.beforeUpdate`, `MutableState.afterUpdate` and `MutableState.withSetter` setter interceptors (similar to the existing `MutableStateFlow` based API).
* Added `State.toMutable` which can turn a `State` into a `MutableState` with a custom setter (similar to the existing `StateFlow` based API).

## 5.10.0

* Improved experimental Jetpack Compose APIs (`ReactiveViewModel`, `by reactiveViewModel`).
* Support for Compose APIs on iOS and JS targets.

## 5.9.0

* Added `JvmSerializable`. Deprecated `Serializable` which is now an alias for `JvmSerializable`.
* Added `RawSerializer` interface which can serialize an object to/from `ByteArray`.
* Added `JvmSerializerReplacement` which can be used with `writeReplace()`.

## 5.8.1

* Build reactivestate-compose for all targets.
* Added WASM target.

## 5.8.0

* Upgraded to Kotlin 2.0.0.

## 5.7.0

* Upgraded to Kotlin 1.9.22.
* Switched `derived` and `AutoRunner` default dispatcher to `dispatcher.main`, to prevent threading errors on iOS by default.
* Added `MutableStateFlow<T>.onStateSubscription {}` and `StateFlow<T>.onStateSubscription {}` which behave like `onSubscription` but return a `MutableStateFlow`/`StateFlow`.

## 5.6.0

* Upgraded to kotlinx.coroutines 1.7.3.
* Added `MutableStateFlow<Int>.incrementFrom(MutableStateFlow<Boolean>)`.

## 5.5.7

* Fixed rare multi-threading race condition during initialization of (Co)AutoRunner where the worker coroutine would trigger the listener before the constructor has finished executing.

## 5.5.6

* Fixed error propagation for `CoroutineLauncher.derived`.

## 5.5.5

* Made `stateOnDemand` emit current value directly on start by default, so the flow doesn't have to do it. Pass `emitValueOnStart = false` as an optimization in case you emit the first value yourself.

## 5.5.4

* Made `.value` access on `derived` and `stateOnDemand` synchronously return/recompute an up-to-date value. This prevents race conditions when mutating MutableStateFlows and directly reading derived values.

## 5.5.3

* Fixed race condition where suspend-based `derived` would do the first evaluation outside of the flow coroutine, thus preventing cancellation on value change when using `transformLatest` or other `...Latest` based workers.

## 5.5.2

* Downgraded to Gradle 7 since some libraries aren't ready yet for JDK 17

## 5.5.1

* Simplified `derived` implementation

## 5.5.0

* `derived` without `CoroutineScope` now computes on demand only

## 5.4.2

* Downgraded to coroutines 1.6.4 to prevent build failures with Ktor.

## 5.4.1

* Fixed `LifecycleStateFlow` and indirectly `autoRun` and `derived`
* Added `runWithResolver`/`coRunWithResolver` helpers for evaluating an `AutoRunner` observer block without subscribing.

## 5.4.0

* Upgraded to Kotlin 1.8.21 which fixes the compilation speed regression.
* Added `Flow<T>.stateOnDemand { ...getter... }` which creates a `StateFlow` without requiring a `CoroutineScope` (unlike `stateIn`).
* Added `Flow<T>.shareOnDemand()` and `sharedFlow {} which creates a `SharedFlow` without requiring a `CoroutineScope` (unlike `shareIn`).
* Added `LifecycleStateFlow` for observing the lifecycle state as a `StateFlow`.
* Added `LifecycleOwner.launchOnceStateAtLeast` which can be used in place of Android's deprecated `launchWhenStarted` etc.
* Added `LifecycleOwner.onceStateAtLeast` which can be used in place of Android's deprecated `whenStarted` etc. and which can run a `suspend fun` unlike Android's `withStateAtLeast`.
* Re-added Jetpack Compose helpers. Currently only available for Android.
* Added `StateFlow.toMutable` which can turn a `StateFlow` into a `MutableStateFlow` with a custom setter.

## 5.3.0

* Added `T.runCatchingNonFatal`. Contributed by @brudaswen. Thank you!

## 5.2.1

* Made `Wrapped` a `Serializable` on JVM.
* Added `Serializable` interface that maps to the JVM `Serializable` and is usable from `commonMain`.

## 5.2.0

* Extracted non-Android code into reactivestate-core(-test) modules, so you can use ReactiveState with minimal dependencies.
* Added `MutableStateFlow.afterUpdate` to complete the API introduced in version 5.1.0.

## 5.1.3

* Upgraded to Kotlin 1.7.21.

## 5.1.2

* Downgraded to Kotlin 1.7.20 because of huge compilation speed degradation on iOS targets and incompatibility with Compose.

## 5.1.1

* Improved Swift compatibility of `eventNotifier` by making the lambdas non-`suspend`.

## 5.1.0

* Added `MutableStateFlow.beforeUpdate` and `MutableStateFlow.withSetter` setter interceptors.
* Upgraded to Kotlin 1.8.0.
* Due to the deprecation of the old JS compiler in Kotlin 1.8.0 the code has switched to the IR compiler.

## 5.0.2

* Upgraded to Kotlin 1.7.20, kotlinx.coroutines 1.6.4, androidx.activity 1.6.0, androidx.fragment 1.5.3.
* Upgraded to Android SDK 33.
* The experimental reactivestate-compose module is not published in this release due to a Kotlin/Compose compiler bug.

## 5.0.1

* Adjusted `EventNotifierTest`/`ReactiveStateTest` to be consistent with the coroutines test lib behavior. You have to call `runCurrent()` to execute any coroutines launched from the ViewModel's `init` block.

## 5.0.0

* Upgraded to kotlinx.coroutines 1.6.1. You might need to adjust your unit tests to take behavioral differences into account.
* Upgraded to Jetpack Compose 1.1.1.
* Upgraded to Kotlin 1.6.20.
* `CoroutineTest.runBlockingTest` was deprecated and you should now use `CoroutineTest.runTest`. Similarly, `testCoroutineScope` => `testScope` and `testCoroutineDispatcher` => `testDispatcher`.
* Use `CoroutineTest.testScope` only for coroutines that will terminate. For `ReactiveStateTest` you should use `CoroutineTest.mainScope` which gets canceled at the end of each test run.
* Native targets are compiled using the experimental memory manager.
* The reactivestate-test module can now be used with all targets instead of just JVM.
* Added a few more targets like macosArm64 and iosSimulatorArm64.
* Moved `get(LiveData)` to the `com.ensody.reactivestate` package, so you don't need to think twice about where you want to import from.

## 4.7.0

* Added `Throwable.throwIfFatal()`, `Throwable.isFatal()` and `runCatchingNonFatal` to deal with fatal errors like `CancellationException` in a simpler and platform-specific way.

## 4.6.2

* Fixed `derived` not replaying the current value.

## 4.6.1

* Fixed `derived` and `derivedWhileSubscribed` bug when used with multiple collectors.
* Fixed `derived` with `WhileSubscribed` subscribe/unsubscribe/re-subscribe bug.

## 4.6.0

* Added `derived` and `derivedWhileSubscribed` variants which don't need a `CoroutineScope`, so you don't need to infect your business logic with scopes.

## 4.5.0

* Upgraded to Kotlin 1.6.10 and Jetpack Compose 1.1.0-rc01.
* Internal improvements.

## 4.4.0

* Added experimental Jetpack Compose support (currently Android-only). By adding `com.ensody.reactivestate:reactivestate-compose` to your dependencies and you can use the `viewModel { MyViewModel() }` and `reactiveState { ... }` `Composable`s.

## 4.3.2

* Fixed race condition with `autoRun`'s observables being changed during run.

## 4.3.1

* Fixed race condition with `derived` where the observed `StateFlow` gets changed during first run.

## 4.3.0

* Added `@DependencyAccessor` annotation to ensure the DI pattern is respected instead of the service locator pattern.

## 4.2.1

* Fixed recursively changing the value of a `MutableValueFlow` from within the `collect` block (change->emit->change). Previously this could lead to a deadlock because the first change is still locking the `MutableValueFLow`.

## 4.2.0

* Added `OnReactiveStateAttached` interface to allow customizing `by reactiveState` behavior.

## 4.1.0

* Added `MutableValueFlow<Int>.incrementFrom(flow: StateFlow<Int>)` extension function to e.g. sum multiple loading states into one `MutableValueFlow<Int>`.

## 4.0.0

This release adds support for Kotlin Multiplatform and introduces a multiplatform `ReactiveState` ViewModel.

Breaking changes:

* The modules have been restructured and renamed:
  * `reactivestate-bom` (previously dependency-versions-bom)
  * `reactivestate` (previously core and reactivestate)
  * `reactivestate-test` (previously core-test)
* `CoroutineTestRule` is now a simple class that you can either derive from or add as an attribute.
* `CoroutineTest` has become independent of JUnit and inherits from `CoroutineTestRule`. The `coroutineTestRule` attribute has been replaced with direct `testCoroutineScope` and `testCoroutineDispatcher` attributes inherited from the new `CoroutineTestRule`.
* The `withLoading` concept in `autoRun`, `CoroutineLauncher` etc. has become more flexible to allow tracking separate loading states.
* Removed `ReducingStateFlow` as part of the simplified loading state concept.

Non-breaking changes:

* Added a multiplatform ViewModel `ReactiveState` (interface), `BaseReactiveState` (base class). This is actually a broader concept that can be used for any living object that can launch coroutines, automatically handles errors, triggers events, and tracks loading states.
* Added a multiplatform `buildViewModel` extension function for creating such a `ViewModel` on an Activity and Fragment.
* Improved automatic error catching for `autoRun` and `derived`.
* Fixes for lifecycle observers: `onCreate`, `onCreateView`, `onCreateViewOnce`, `onDestroyView`, `onDestroyViewOnce`.
* `MutableValueFlow.replaceLocked` returns the previous value now.

Known limitations which will be solved with later releases:

* On non-JVM platforms, `dispatchers.io` currently equals `Dispatchers.Default`.
* This primarily affects `MutableValueFlow`: Internally, all uses of the JVM-only `synchronized` have been replaced with a spinlock `Mutex` since they were only utilized for very tiny blocks of code which normally don't even have any parallel access. Be careful about doing too large computations in combination with highly concurrent updates via `replaceLocked`, though.

Changelog of preview releases:

* 4.0.0-dev.4:
  * Fixed build failures due to Jacoco integration.
* 4.0.0-dev.3:
  * Removed `ReducingStateFlow`.
  * Publish iOS/macOS builds.
* 4.0.0-dev.2:
  * Improved automatic error catching for `autoRun` and `derived`.
  * Fixes for lifecycle observers
  * Removed `LoadingStateTracker`.
  * Replaced `CoroutineLauncher.isAnyLoading` and `generalLoading` with a simple `loading: MutableValueFlow<Int>`.
  * `MutableValueFlow.replaceLocked` returns the previous value now.
  * `MutableValueFlow.increment`/`decrement` now have an optional `amount` argument to increment by more than 1. Also, they return the previous value.
* 4.0.0-dev.1: This preview release comes without macOS/iOS builds. A port of the CI pipeline is in progress.

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
