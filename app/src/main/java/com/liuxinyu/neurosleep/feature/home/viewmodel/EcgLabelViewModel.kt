package com.liuxinyu.neurosleep.feature.home.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liuxinyu.neurosleep.data.model.EcgLabel
import com.liuxinyu.neurosleep.data.repository.EcgLabelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// EcgLabelViewModel.kt
class EcgLabelViewModel(private val repository: EcgLabelRepository) : ViewModel() {
    private val _labels = MutableStateFlow<List<EcgLabel>>(emptyList())
    val labels: StateFlow<List<EcgLabel>> = _labels

    init {
        viewModelScope.launch {
            try {
                // 立即开始收集数据
                repository.getLabelsFlow().collect { loadedLabels ->
                    Log.d("EcgLabelViewModel", "Loaded ${loadedLabels.size} labels from repository")
                    _labels.value = loadedLabels
                }
            } catch (e: Exception) {
                Log.e("EcgLabelViewModel", "Error collecting labels", e)
            }
        }
    }

    fun addLabel(label: EcgLabel) {
        viewModelScope.launch {
            try {
                // 检查是否已存在相同开始时间的标签
                val existingLabel = _labels.value.find { it.startTime == label.startTime }
                if (existingLabel == null) {
                    // 只有在不存在相同标签时才添加
                    _labels.update { current ->
                        current + label
                    }
                    saveToRepository()
                    Log.d("EcgLabelViewModel", "Added new label: ${label.labelType}")
                } else {
                    Log.d("EcgLabelViewModel", "Label with startTime ${label.startTime} already exists")
                }
            } catch (e: Exception) {
                Log.e("EcgLabelViewModel", "Error adding label", e)
            }
        }
    }

    fun updateLabel(updatedLabel: EcgLabel) {
        viewModelScope.launch {
            try {
                _labels.update { current ->
                    current.map { label ->
                        if (label.startTime == updatedLabel.startTime) updatedLabel else label
                    }
                }
                saveToRepository()
                Log.d("EcgLabelViewModel", "Updated label: ${updatedLabel.labelType}")
            } catch (e: Exception) {
                Log.e("EcgLabelViewModel", "Error updating label", e)
            }
        }
    }

    fun deleteLabel(label: EcgLabel) {
        viewModelScope.launch {
            try {
                // 先从数据库中删除
                repository.deleteLabel(label)
                // 然后更新本地状态
                _labels.update { current ->
                    current.filter { it.startTime != label.startTime }
                }
                Log.d("EcgLabelViewModel", "Deleted label: ${label.labelType}")
            } catch (e: Exception) {
                Log.e("EcgLabelViewModel", "Error deleting label", e)
            }
        }
    }

    private fun saveToRepository() {
        viewModelScope.launch {
            try {
                repository.saveLabels(_labels.value)
                Log.d("EcgLabelViewModel", "Saved ${_labels.value.size} labels to repository")
            } catch (e: Exception) {
                Log.e("EcgLabelViewModel", "Error saving labels to repository", e)
            }
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            try {
                repository.clearUserLabels()
                _labels.value = emptyList()
                Log.d("EcgLabelViewModel", "Cleared all labels")
            } catch (e: Exception) {
                Log.e("EcgLabelViewModel", "Error clearing session", e)
            }
        }
    }
}