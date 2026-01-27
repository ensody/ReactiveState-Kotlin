package com.ensody.reactivestate.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.ensody.reactivestate.DIImpl
import com.ensody.reactivestate.DIResolver
import com.ensody.reactivestate.ExperimentalReactiveStateApi
import com.ensody.reactivestate.qualifiedNameOrSimpleName

@ExperimentalReactiveStateApi
@Composable
public inline fun <reified T> DIImpl.derivedValue(key: Any? = null, crossinline block: DIResolver.() -> T): T =
    derivedState(key = key) { block() }.value

@ExperimentalReactiveStateApi
@Composable
public inline fun <reified T> DIImpl.derivedState(key: Any? = null, crossinline block: DIResolver.() -> T): State<T> =
    rememberOnViewModel(key = "${T::class.qualifiedNameOrSimpleName}|$key") {
        this@derivedState.derived { block() }
    }.collectAsState()
