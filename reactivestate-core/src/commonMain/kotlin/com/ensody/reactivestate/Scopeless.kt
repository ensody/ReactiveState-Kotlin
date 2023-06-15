package com.ensody.reactivestate

import kotlinx.coroutines.MainScope

// Used for the derived version which doesn't need a CoroutineScope
internal val mainScope by lazy { MainScope() }
internal val scopelessCoroutineLauncher by lazy { SimpleCoroutineLauncher(mainScope) }
