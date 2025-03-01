package com.example.playermss.data

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SomeViewModel<T>(private val t:T): ViewModel() {
    private val _state = MutableStateFlow<T?>(t)
    val state = _state.asStateFlow()

    fun set(t:T){
        _state.value = t
    }
}