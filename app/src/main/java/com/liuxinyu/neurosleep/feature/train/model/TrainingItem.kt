package com.liuxinyu.neurosleep.feature.train.model

/**
 * 训练项目的媒体类型
 */
enum class MediaType {
    AUDIO,
    VIDEO
}

/**
 * Data class representing a training item
 *
 * @property id Unique identifier for the training
 * @property title Title of the training
 * @property description Description of the training
 * @property imageResId Resource ID for the training thumbnail image
 * @property mediaResId Resource ID for the training media file (audio or video)
 * @property mediaType Type of media (AUDIO or VIDEO)
 * @property duration Duration of the training in minutes
 * @property category The category this training belongs to
 */
data class TrainingItem(
    val id: Int,
    val title: String,
    val description: String,
    val imageResId: Int,
    val mediaResId: Int,
    val mediaType: MediaType = MediaType.AUDIO,
    val duration: Int,
    val category: TrainingCategory
)

/**
 * Enum representing different training categories
 */
enum class TrainingCategory {
    RELAXATION,
    MOTIVATION,
    COGNITIVE,
    ASSESSMENT,
    MEDITATION
} 