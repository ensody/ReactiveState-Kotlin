# ReactiveState

Kotlin Multiplatform ViewModels and reactive state management based on `StateFlow`.

See the `reactivestate-core` module documentation as a starting point with several usage examples.

## Short examples

Map/transform a StateFlow:

```kotlin
val number = MutableStateFlow(0)
val doubledNumber: StateFlow<Int> = derived { 2 * get(number) }
```

Collect two StateFlows (just collect without transforming):

```kotlin
val base = MutableStateFlow(0)
val extra = MutableStateFlow(0)
autoRun {
    if (get(base) + get(extra) > 10) {
        alert("You're flying too high")
    }
}
```

Multiplatform ViewModels with automatic error handling and loading indicator tracking:

```kotlin
class ExampleViewModel(scope: CoroutineScope, val repository: ExampleRepository) : ReactiveViewModel(scope) {
    val inputFieldValue = MutableStateFlow("default")

    fun submit() {
        // The launch function automatically catches exceptions and increments/decrements the loading indicator.
        // This way you can't forget the fundamentals that have to be always handled correctly.
        launch {
            repository.submit(inputFieldValue.value)
        }
    }
}
```

Intercept MutableStateFlow:

```kotlin
public val state: MutableStateFlow<String> = MutableStateFlow("value").afterUpdate {
    // This is called every time after someone sets `state.value = ...`
}
```

Convert StateFlow to MutableStateFlow:

```kotlin
val readOnly: StateFlow<Int> = getSomeStateFlow()
val mutable: MutableStateFlow<Int> = readOnly.toMutable { value: Int ->
    // This is executed whenever someone sets `mutable.value = ...`.
}
```

## Supported platforms

android, jvm, ios, tvos, watchos, macosArm64, macosX64, mingwX64, linuxX64

## Installation

Add the package to your `build.gradle`:

```groovy
dependencies {
    // Add the BOM using the desired ReactiveState version
    api platform("com.ensody.reactivestate:reactivestate-bom:VERSION")
    // Leave out the version number from now on.

    // Jetpack Compose integration
    implementation "com.ensody.reactivestate:reactivestate-compose"

    // Android-only integration for Activity/Fragment
    implementation "com.ensody.reactivestate:reactivestate-android"

    // UI-independent core APIs
    implementation "com.ensody.reactivestate:reactivestate-core"

    // Utils for unit tests that want to use coroutines
    implementation "com.ensody.reactivestate:reactivestate-core-test"

    // Android-only unit test extensions
    implementation "com.ensody.reactivestate:reactivestate-android-test"
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
