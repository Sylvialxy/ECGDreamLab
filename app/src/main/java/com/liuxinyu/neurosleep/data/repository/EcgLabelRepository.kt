package com.liuxinyu.neurosleep.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.liuxinyu.neurosleep.data.database.EcgLabelConverter
import com.liuxinyu.neurosleep.data.database.EcgLabelDao
import com.liuxinyu.neurosleep.data.model.EcgLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.format.DateTimeFormatter

// EcgLabelRepository.kt
class EcgLabelRepository private constructor(
    private val dao: EcgLabelDao,
    private val context: Context
) {
    private val currentSessionId = MutableStateFlow<String?>(null)
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private val MAX_RECORDS = 100 // 限制最大记录数
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("ecg_prefs", Context.MODE_PRIVATE)
    }

    init {
        // 从 SharedPreferences 恢复 sessionId
        val savedSessionId = prefs.getString("current_session_id", null)
        Log.d("EcgLabelRepository", "Initializing with saved sessionId: $savedSessionId")
        currentSessionId.value = savedSessionId
    }

    companion object {
        @Volatile
        private var INSTANCE: EcgLabelRepository? = null

        fun getInstance(dao: EcgLabelDao, context: Context): EcgLabelRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = EcgLabelRepository(dao, context)
                INSTANCE = instance
                instance
            }
        }
    }

    // 初始化时设置用户ID
    fun setUserId(phone: String) {
        Log.d("EcgLabelRepository", "Setting user ID: $phone")
        currentSessionId.value = phone
        // 保存到 SharedPreferences
        prefs.edit().putString("current_session_id", phone).apply()
    }

    // 保存当次会话标签
    suspend fun saveLabels(labels: List<EcgLabel>) {
        val sessionId = currentSessionId.value ?: throw IllegalStateException("User ID not set")
        Log.d("EcgLabelRepository", "Saving ${labels.size} labels for session: $sessionId")
        try {
            for (label in labels) {
                val entity = EcgLabelConverter.toEntity(label, sessionId)
                // 检查是否存在相同开始时间的记录
                val existingEntity = dao.getLabelByStartTime(sessionId, entity.startTime)
                if (existingEntity != null) {
                    // 如果存在，更新记录
                    dao.updateLabel(entity.copy(id = existingEntity.id))
                } else {
                    // 如果不存在，插入新记录
                    dao.insertLabels(listOf(entity))
                }
            }
            Log.d("EcgLabelRepository", "Successfully saved labels to database")
        } catch (e: Exception) {
            Log.e("EcgLabelRepository", "Error saving labels", e)
            throw e
        }
    }

    // 返回 Flow 以实现实时更新
    fun getLabelsFlow(): Flow<List<EcgLabel>> {
        return currentSessionId.flatMapLatest { sessionId ->
            if (sessionId == null) {
                Log.w("EcgLabelRepository", "No session ID available")
                flowOf(emptyList())
            } else {
                Log.d("EcgLabelRepository", "Getting labels for session: $sessionId")
                dao.getLabelsBySessionFlow(sessionId).map { entities ->
                    try {
                        // 只取最新的 MAX_RECORDS 条记录
                        val labels = entities.takeLast(MAX_RECORDS).map {
                            EcgLabelConverter.fromEntity(
                                it
                            )
                        }
                        Log.d("EcgLabelRepository", "Retrieved ${labels.size} labels")
                        labels
                    } catch (e: Exception) {
                        Log.e("EcgLabelRepository", "Error converting entities to labels", e)
                        emptyList()
                    }
                }
            }
        }
    }

    // 清除当前用户的标签数据
    suspend fun clearUserLabels() {
        currentSessionId.value?.let { sessionId ->
            Log.d("EcgLabelRepository", "Clearing labels for session: $sessionId")
            try {
                dao.clearSession(sessionId)
                Log.d("EcgLabelRepository", "Successfully cleared labels")
            } catch (e: Exception) {
                Log.e("EcgLabelRepository", "Error clearing labels", e)
                throw e
            }
        }
    }

    // 删除单个标签
    suspend fun deleteLabel(label: EcgLabel) {
        val sessionId = currentSessionId.value ?: throw IllegalStateException("User ID not set")
        try {
            val entity = EcgLabelConverter.toEntity(label, sessionId)
            dao.deleteLabel(entity)
            Log.d("EcgLabelRepository", "Successfully deleted label")
        } catch (e: Exception) {
            Log.e("EcgLabelRepository", "Error deleting label", e)
            throw e
        }
    }

    // 获取当前会话ID
    fun getCurrentSessionId(): String? {
        return currentSessionId.value
    }
}