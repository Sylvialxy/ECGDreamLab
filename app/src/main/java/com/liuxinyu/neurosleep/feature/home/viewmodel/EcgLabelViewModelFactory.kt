package com.liuxinyu.neurosleep.feature.home.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liuxinyu.neurosleep.data.repository.EcgLabelRepository

class EcgLabelViewModelFactory(
    private val repository: EcgLabelRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EcgLabelViewModel::class.java)) {
            return EcgLabelViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

