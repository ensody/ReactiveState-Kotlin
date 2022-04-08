package com.ensody.reactivestate.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ensody.reactivestate.BaseReactiveState
import com.ensody.reactivestate.ErrorEvents
import com.ensody.reactivestate.OnReactiveStateAttached
import com.ensody.reactivestate.ReactiveState
import com.ensody.reactivestate.StateFlowStore
import kotlinx.coroutines.CoroutineScope

internal class TestViewModel(scope: CoroutineScope, val store: StateFlowStore) : BaseReactiveState<ErrorEvents>(scope) {
    val name = store.getData("name", "")
    val count = store.getData("count", 0)
    val nullableName = store.getData<String?>("name", null)

    fun throwError() {
        launch {
            throw TestException()
        }
    }
}

internal class TestException : RuntimeException()

internal class TestFragment : Fragment(), ErrorEvents, OnReactiveStateAttached {
    internal val viewModel by reactiveState { TestViewModel(scope, stateFlowStore) }

    internal lateinit var textView: TextView

    internal val errors = mutableListOf<Throwable>()
    internal val attachedReactiveStates = mutableListOf<Any>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        textView = TextView(context)
        return textView
    }

    override fun onError(error: Throwable) {
        errors.add(error)
    }

    override fun onReactiveStateAttached(reactiveState: ReactiveState<out ErrorEvents>) {
        attachedReactiveStates.add(reactiveState)
    }
}
