package com.ensody.reactivestate.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ensody.reactivestate.BaseReactiveState
import com.ensody.reactivestate.ErrorEvents
import com.ensody.reactivestate.StateFlowStore
import kotlinx.coroutines.CoroutineScope

internal class TestViewModel(scope: CoroutineScope, val store: StateFlowStore) : BaseReactiveState<ErrorEvents>(scope) {
    val name = store.getData("name", "")
    val count = store.getData("count", 0)
    val nullableName = store.getData<String?>("name", null)
}

internal class TestFragment : Fragment() {
    internal val viewModel by reactiveState { TestViewModel(scope, stateFlowStore) }

    internal lateinit var textView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        textView = TextView(context)
        return textView
    }
}
