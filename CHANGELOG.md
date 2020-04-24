# Change log

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
