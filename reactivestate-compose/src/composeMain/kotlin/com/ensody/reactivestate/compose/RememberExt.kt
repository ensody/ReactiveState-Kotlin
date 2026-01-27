package com.ensody.reactivestate.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ensody.reactivestate.ExperimentalReactiveStateApi
import com.ensody.reactivestate.InternalReactiveStateApi
import com.ensody.reactivestate.qualifiedNameOrSimpleName

@ExperimentalReactiveStateApi
@OptIn(InternalReactiveStateApi::class)
@Composable
public inline fun <reified T> rememberOnViewModel(key: Any? = null, crossinline block: () -> T): T {
    val realKey = key ?: currentCompositeKeyHashCode
    val fullKey = "rememberOnViewModel:${T::class.qualifiedNameOrSimpleName}:$realKey"
    return viewModel(key = fullKey) { RememberViewModel(block()) }.value
}

@InternalReactiveStateApi
public class RememberViewModel<T>(public val value: T) : ViewModel()
