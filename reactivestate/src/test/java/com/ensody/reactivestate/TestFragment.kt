package com.ensody.reactivestate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

class TestState(scope: CoroutineScope, store: StateFlowStore) : Scoped(scope) {
    val name = store.getData("name", "")
    val count = store.getData("count", 0)
}

class TestViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val state = TestState(viewModelScope, SavedStateHandleStore(viewModelScope, savedStateHandle))
}

class TestFragment : Fragment() {
    internal val model by stateViewModel { TestViewModel(it) }
    internal val state get() = model.state

    internal lateinit var textView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        textView = TextView(context)
        return textView
    }
}