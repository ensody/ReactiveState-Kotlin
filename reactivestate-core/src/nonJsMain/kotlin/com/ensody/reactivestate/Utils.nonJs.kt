package com.ensody.reactivestate

import kotlin.reflect.KClass

public actual val KClass<*>.qualifiedNameOrSimpleName: String? get() = qualifiedName ?: simpleName
