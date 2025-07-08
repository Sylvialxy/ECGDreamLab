package com.liuxinyu.neurosleep.feature.train.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.feature.train.model.MediaType
import com.liuxinyu.neurosleep.feature.train.model.TrainingCategory
import com.liuxinyu.neurosleep.feature.train.model.TrainingItem

class TrainingViewModel : ViewModel() {

    private val _trainingItems = MutableLiveData<List<TrainingItem>>()
    val trainingItems: LiveData<List<TrainingItem>> = _trainingItems

    private val _selectedCategory = MutableLiveData<TrainingCategory>()
    val selectedCategory: LiveData<TrainingCategory> = _selectedCategory

    // Mock data for each category
    private val allTrainingItems = listOf(
        // Relaxation training
        TrainingItem(
            id = 1,
            title = "深度呼吸放松",
            description = "通过控制呼吸节奏达到身心放松的效果",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 10,
            category = TrainingCategory.RELAXATION
        ),
        TrainingItem(
            id = 2,
            title = "渐进性肌肉放松",
            description = "通过紧张和放松不同肌肉群来减轻身体紧张",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 15,
            category = TrainingCategory.RELAXATION
        ),
        TrainingItem(
            id = 11,
            title = "放松训练视频教程",
            description = "视频指导如何进行全身放松训练",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.openandcloseeyes_breathingtraining,
            mediaType = MediaType.VIDEO,
            duration = 8,
            category = TrainingCategory.RELAXATION
        ),
        
        // Motivation training
        TrainingItem(
            id = 3,
            title = "目标设定",
            description = "如何设定和实现个人目标的引导训练",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 12,
            category = TrainingCategory.MOTIVATION
        ),
        TrainingItem(
            id = 4,
            title = "内在动力激发",
            description = "探索并唤醒你内在的动力和热情",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 18,
            category = TrainingCategory.MOTIVATION
        ),
        TrainingItem(
            id = 12,
            title = "激励视频课程",
            description = "视频演示如何保持积极心态",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_video,
            mediaType = MediaType.VIDEO,
            duration = 10,
            category = TrainingCategory.MOTIVATION
        ),
        
        // Cognitive training
        TrainingItem(
            id = 5,
            title = "专注力训练",
            description = "增强注意力和专注能力的练习",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 10,
            category = TrainingCategory.COGNITIVE
        ),
        TrainingItem(
            id = 6,
            title = "记忆力强化",
            description = "提高记忆力和信息处理能力的练习",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 15,
            category = TrainingCategory.COGNITIVE
        ),
        TrainingItem(
            id = 13,
            title = "认知训练视频",
            description = "通过视频学习认知训练方法",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_video,
            mediaType = MediaType.VIDEO,
            duration = 12,
            category = TrainingCategory.COGNITIVE
        ),
        
        // Assessment
        TrainingItem(
            id = 7,
            title = "睡眠质量评估",
            description = "评估你当前的睡眠质量和模式",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 8,
            category = TrainingCategory.ASSESSMENT
        ),
        TrainingItem(
            id = 8,
            title = "压力水平测试",
            description = "测量并了解你当前的压力水平",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 12,
            category = TrainingCategory.ASSESSMENT
        ),
        
        // Meditation
        TrainingItem(
            id = 9,
            title = "正念冥想",
            description = "培养专注于当下时刻的能力",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 15,
            category = TrainingCategory.MEDITATION
        ),
        TrainingItem(
            id = 10,
            title = "引导式冥想",
            description = "通过引导式冥想缓解压力和焦虑",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_audio,
            mediaType = MediaType.AUDIO,
            duration = 20,
            category = TrainingCategory.MEDITATION
        ),
        TrainingItem(
            id = 14,
            title = "冥想呼吸视频教程",
            description = "视频教学正确的冥想呼吸方法",
            imageResId = R.drawable.sleepicon,
            mediaResId = R.raw.sample_video,
            mediaType = MediaType.VIDEO,
            duration = 15,
            category = TrainingCategory.MEDITATION
        )
    )

    init {
        // Set initial category to RELAXATION
        setSelectedCategory(TrainingCategory.RELAXATION)
    }

    fun setSelectedCategory(category: TrainingCategory) {
        _selectedCategory.value = category
        _trainingItems.value = allTrainingItems.filter { it.category == category }
    }
} 