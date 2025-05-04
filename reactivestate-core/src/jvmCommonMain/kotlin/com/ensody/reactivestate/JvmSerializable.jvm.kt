package com.ensody.reactivestate

import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

public actual typealias JvmSerializable = java.io.Serializable

@Suppress("UNCHECKED_CAST")
public actual fun <T : Any> JvmSerializerReplacement(serializer: RawSerializer<T>, value: T): Any =
    DefaultJvmSerializerReplacement(serializer, value)

@PublishedApi // IMPORTANT: changing the class name would result in serialization incompatibility
internal class DefaultJvmSerializerReplacement<T : Any>(
    private var serializer: RawSerializer<T>?,
    private var value: T?,
) : Externalizable {
    constructor() : this(null, null)

    override fun writeExternal(out: ObjectOutput) {
        out.writeObject(serializer)
        @Suppress("UnsafeCallOnNullableType")
        out.writeObject(serializer!!.rawSerialize(value!!))
    }

    @Suppress("UNCHECKED_CAST")
    override fun readExternal(`in`: ObjectInput) {
        serializer = `in`.readObject() as RawSerializer<T>
        @Suppress("UnsafeCallOnNullableType")
        value = serializer!!.rawDeserialize(`in`.readObject() as ByteArray)
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun readResolve(): Any =
        value!!

    companion object {
        private const val serialVersionUID: Long = 0L
    }
}
