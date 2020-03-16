package com.ensody.reactivestate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class TestViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val name = savedStateHandle.getLiveDataNonNull("name", "")
    val count = savedStateHandle.getLiveDataNonNull("count", 0)
}

class TestFragment : Fragment() {
    internal val model by stateViewModel { TestViewModel(it) }

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