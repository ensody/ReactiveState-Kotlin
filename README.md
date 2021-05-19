# ReactiveState for Kotlin Multiplatform and Android

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ensody.reactivestate/reactivestate/badge.svg?gav=true)](https://maven-badges.herokuapp.com/maven-central/com.ensody.reactivestate/reactivestate?gav=true)

Easy reactive state management for Kotlin Multiplatform. No boilerplate. Compatible with Android.

ReactiveState-Kotlin provides these foundations:

* [reactive programming](https://ensody.github.io/ReactiveState-Kotlin/reactive-programming/): everything is recomputed/updated automatically based on straightforward code
* [demand-driven programming](https://ensody.github.io/ReactiveState-Kotlin/demand-driven-programming/): resource-consuming computations and values are allocated on-demand and disposed when not needed
* [multiplatform](https://ensody.github.io/ReactiveState-Kotlin/multiplatform-viewmodels/): share your ViewModels and reactive state handling logic between all platforms
* [event handling](https://ensody.github.io/ReactiveState-Kotlin/event-handling/): simple events based on interfaces (more composable and less boilerplate than sealed classes)
* [automatic error catching](https://ensody.github.io/ReactiveState-Kotlin/error-handling/): no more forgotten try-catch or copy-pasted error handling logic all over the place
* [coroutine-based unit tests](https://ensody.github.io/ReactiveState-Kotlin/unit-testing-coroutines/): worry no more about passing around `CoroutineDispatcher`s everywhere
* [lifecycle handling](https://ensody.github.io/ReactiveState-Kotlin/lifecycle-handling/)
* [state restoration](https://ensody.github.io/ReactiveState-Kotlin/state-restoration/)

See the [ReactiveState documentation](https://ensody.github.io/ReactiveState-Kotlin/) for more details.

## Supported platforms

android, jvm, ios, tvos, watchos, macosX64, linuxX64, js

## Installation

Add the package to your `build.gradle`'s `dependencies {}`:

```groovy
dependencies {
    // Add the BOM using the desired ReactiveState version
    api platform("com.ensody.reactivestate:reactivestate-bom:VERSION")

    // Leave out the version number from now on:
    implementation "com.ensody.reactivestate:reactivestate"

    // Utils for unit tests that want to use coroutines
    implementation "com.ensody.reactivestate:reactivestate-test"
    // Note: kotlin-coroutines-test only supports the "jvm" target,
    // so reactivestate-test has the same limitation
}
```

Also, make sure you've integrated the Maven Central repo, e.g. in your root `build.gradle`:

```groovy
subprojects {
    repositories {
        // ...
        mavenCentral()
        // ...
    }
}
```

## Quick intro

The following two principles are here to give you a quick idea of the reactive programming aspect only.
The "Guide" section in the [documentation](https://ensody.github.io/ReactiveState-Kotlin/) describes how to work with the more advanced aspects like multiplatform ViewModels, lifecycle handling, etc.

Note: While the discussion is about `StateFlow`, you can also use `LiveData` or even implement extensions for other observable values.

### Observing StateFlow

Imagine you have an input form with first and last name and want to observe two `StateFlow` values at the same time:

* `isFirstNameValid: StateFlow<Boolean>`
* `isLastNameValid: StateFlow<Boolean>`

This is how you'd do it by using the `autoRun` function:

```kotlin
autoRun {
    submitButton.isEnabled = get(isFirstNameValid) && get(isLastNameValid)
}
```

With `get(isFirstNameValid)` you retrieve `isFirstNameValid.value` and at the same time tell `autoRun` to re-execute the block whenever the value is changed.
That code is similar to writing this:

```kotlin
lifecycleScope.launchWhenStarted {
    isFirstNameValid
        .combine(isLastNameValid) { firstNameValid, lastNameValid ->
            firstNameValid to lastNameValid
        }
        .conflate()
        .collect { (firstNameValid, lastNameValid) ->
            try {
                submitButton.isEnabled = firstNameValid && lastNameValid
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError(e)
            }
        }
}
```

### Reactive StateFlow / reactive data

The same principle can be used to create a `derived`, reactive `StateFlow`:

```kotlin
val isFormValid: StateFlow<Boolean> = derived {
    get(isFirstNameValid) && get(isLastNameValid)
}
```

Now you can use `autoRun { submitButton.isEnabled = get(isFormValid) }` in the rest of your code.

Going even further, `isFirstNameValid` itself would usually also be the result of a `derived` computation.
So, you can have multiple layers of reactive `derived` `StateFlow`s.

## Relation to Jetpack Compose / Flutter / React

Reactive UI frameworks like Jetpack Compose automatically rebuild the UI whenever e.g. a `StateFlow` changes.
Isn't that the same thing as `autoRun` already? Do you still need this library?

If you look closely at the code sample above, the ViewModel uses `derived` to automatically recompute a `StateFlow` based on other `StateFlow`s.
This pattern is very useful in practice and provides the perfect foundation for frameworks like Jetpack Compose which only focus on the UI aspect.
Actually, Jetpack Compose is like `derived` for the UI.
ReactiveState's `derived` and `autoRun` provide the same reactivity for your data and business logic.

So, the combination of both solutions used together results in a fully reactive codebase - which improves code simplicity and avoids many bugs.

Moreover, Jetpack Compose currently doesn't provide any multiplatform ViewModel support or any large-scale architecture.
So, this library solves this by providing `BaseReactiveState` for ViewModels.
This comes with a lifecycle-aware event system (`eventNotifier`) and loading state handling (so you can track one or multiple different loading indicators based on coroutines that you launch).

## See also

This library is based on [reactive_state](https://github.com/ensody/reactive_state) for Flutter and adapted to Kotlin Multiplatform and Android patterns.

## License

```
Copyright 2020-2021 Ensody GmbH, Waldemar Kornewald

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
