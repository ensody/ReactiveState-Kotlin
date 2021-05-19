# MutableValueFlow

`MutableValueFlow` implements the same API as `MutableStateFlow`, but also provides some extras:

The `replaceLocked` method allows safely replacing the current `value` under concurrent updates:

```kotlin
// SubValue1 and SubValue2 are other data class
data class SomeValue(val subvalue1: SubValue1, val subvalue2: SubValue2, val isLoading: Boolean)

val valueFlow = MutableValueFlow(SomeValue(isLoading = false, /* ... */))

valueFlow.replaceLocked { copy(isLoading = true) }
// now valueFlow.value.isLoading is true
```

Note: ReactiveState also provides an extension function, `replace`, which is defined on `MutableStateFlow` (and `MutableValueFlow`) which does the same thing as `replaceLocked`, but without the concurrency safety.

Additionally, `MutableValueFlow` has an `update` method for working with mutable values:

```kotlin
// Now with var instead of val
data class SomeValue(var subvalue1: SubValue1, var subvalue2: SubValue2, var isLoading: Boolean)

// MutableValueFlow

valueFlow.update {
    it.subvalue1.deepsubvalue.somevalue += 3
    it.subvalue2.state = SomeState.IN_PROGRESS
    it.isLoading = true
}

// versus MutableStateFlow

stateFlow.value = flow.value.let {
    it.copy(
        subvalue1 = it.subvalue1.copy(
            deepsubvalue = it.subvalue1.deepsubvalue.copy(somevalue = it.subvalue1.deepsubvalue.somevalue + 3)
         ),
        subvalue2 = it.subvalue2.copy(state = SomeState.IN_PROGRESS),
        isLoading = true,
    )
}
```

If you work with immutable data classes then you might know this problem. You can make immutable data less painful with functional lenses (e.g. [arrow Optics DSL](https://arrow-kt.io/docs/optics/dsl/) and [arrow Lens](https://arrow-kt.io/docs/optics/lens/)), but that can still result in complicated and inefficient code.

On the other hand, mutable data does allow to shoot yourself in the foot. So whether you want to use `MutableValueFlow` is a question of your architecture and code structure and the specific circumstances.
Usually, reactive code consciously puts data into observables (`StateFlow`s) in order to allow for reactivity.
This results in a code structure where these `StateFlow`s are the primary hosts of each piece of data and the mutations are limited around each `StateFlow` or even around the observable database as the single source of truth.

Under these circumstances it can be quite safe to work with mutable data and `MutableValueFlow` makes such use-cases simpler than `MutableStateFlow`.
Sometimes you even have a mutable third-party object (e.g. `AtomicInteger`) that you have to work with and `StateFlow` is impossible to use in those cases.
