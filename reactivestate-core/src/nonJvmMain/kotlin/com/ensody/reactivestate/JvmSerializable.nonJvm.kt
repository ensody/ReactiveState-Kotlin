package com.ensody.reactivestate

public actual interface JvmSerializable

public actual fun <T : Any> JvmSerializerReplacement(serializer: RawSerializer<T>, value: T): Any =
    DummyJvmSimpleSerializerReplacement
