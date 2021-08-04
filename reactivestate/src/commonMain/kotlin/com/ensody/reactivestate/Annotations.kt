package com.ensody.reactivestate

/** This feature is not stable yet and can introduce breaking API changes in minor releases. */
@MustBeDocumented
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(value = AnnotationRetention.BINARY)
public annotation class ExperimentalReactiveStateApi

/** Marks dependency injection system accessors, so direct access must be explicitly opted in. */
@MustBeDocumented
@RequiresOptIn(
    message = "Direct access to the DI causes tight coupling. If possible, use constructor injection or parameters.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(value = AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class DependencyAccessor
