package com.liuxinyu.neurosleep.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// EcgLabelDao.kt
@Dao
interface EcgLabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<EcgLabelEntity>)

    @Query("SELECT * FROM ecg_labels WHERE sessionId = :sessionId ORDER BY startTime DESC")
    fun getLabelsBySessionFlow(sessionId: String): Flow<List<EcgLabelEntity>>

    @Query("SELECT * FROM ecg_labels WHERE sessionId = :sessionId")
    suspend fun getLabelsBySession(sessionId: String): List<EcgLabelEntity>

    @Query("DELETE FROM ecg_labels WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Delete
    suspend fun deleteLabel(label: EcgLabelEntity)

    @Query("DELETE FROM ecg_labels WHERE sessionId = :sessionId AND startTime = :startTime")
    suspend fun deleteLabelByStartTime(sessionId: String, startTime: String)

    @Update
    suspend fun updateLabel(label: EcgLabelEntity)

    @Query("SELECT * FROM ecg_labels WHERE sessionId = :sessionId AND startTime = :startTime LIMIT 1")
    suspend fun getLabelByStartTime(sessionId: String, startTime: String): EcgLabelEntity?
}