# Changelog

## Next release (master)

ATTENTION: This release comes with a few breaking changes.

* Bindings don't have value converters, anymore. Usually you need to store the raw field value and a separate conversion (if possible without errors), anyway. Use `autoRun` or `derived` to convert values.
* Added bindings for `CompoundButton` (replacing `CheckBox` bindings).
* `AutoRunner`'s and `autoRun`'s `observer` callback now receives the `Resolver` via `this` instead of as an argument (more consistent and compact code). You can write `get(livedata)` to retrieve a `LiveData` value.
* `AutoRunner`'s and `autoRun`'s `onChange` callback now receives the `AutoRunner` as its first argument.
* Added `onResume`, `onResumeOnce`, `onPause`, `onPauseOnce`, `launchWhileResumed` lifecycle observers.
* Added `Disposable.disposeOnCompletionOf(coroutineContext)` extension methods.
* Added `WorkQueue` and helpers like `conflatedWorkQueue` for simpler communication between UI and ViewModel.
* Added unit tests.
* Added documentation.

## 0.5

Initial release.
