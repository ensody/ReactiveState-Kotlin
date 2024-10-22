package com.ensody.reactivestate

/** Alias for `java.io.Serializable` on JVM. Empty interface otherwise. */
public expect interface JvmSerializable

@Deprecated(
    "Use JvmSerializable (avoids name clash with kotlinx.serialization)",
    ReplaceWith("JvmSerializable"),
)
public typealias Serializable = JvmSerializable

/**
 * Handles custom serialization/deserialization. Useful e.g. with [JvmSerializerReplacement].
 */
public interface RawSerializer<T> : JvmSerializable {
    public fun rawSerialize(value: T): ByteArray
    public fun rawDeserialize(value: ByteArray): T
}

/**
 * Simplifies implementing custom `java.io.Serializable` ([JvmSerializable]) logic.
 *
 * Your class must inherit from [JvmSerializable] and call this function in the `writeReplace()` function.
 * This must be used in combination with [RawSerializer].
 * Example:
 *
 * ```kotlin
 * class Foo(val url: Url) : JvmSerializable {
 *     private fun writeReplace(): Any = JvmSerializerReplacement(FooJvmSerializer, this)
 * }
 *
 * internal object FooJvmSerializer : JvmSerializer<Foo> {
 *     override fun jvmSerialize(value: Foo): ByteArray =
 *         value.url.toString().encodeToByteArray()
 *
 *     override fun jvmDeserialize(value: ByteArray): Foo =
 *         Foo(Url(value.decodeToString()))
 * }
 * ```
 */
public expect fun <T : Any> JvmSerializerReplacement(serializer: RawSerializer<T>, value: T): Any

internal object DummyJvmSimpleSerializerReplacement
