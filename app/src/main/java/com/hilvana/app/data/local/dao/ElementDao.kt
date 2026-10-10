package com.hilvana.app.data.local.dao

import androidx.room.*
import com.hilvana.app.data.local.entity.ElementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ElementDao {

    @Query("SELECT * FROM elements ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ElementEntity>>

    @Query("SELECT * FROM elements WHERE type = :type ORDER BY updatedAt DESC")
    fun observeByType(type: String): Flow<List<ElementEntity>>

    @Query("SELECT * FROM elements WHERE remindAt IS NOT NULL AND isDone = 0 ORDER BY remindAt ASC")
    fun observePendingReminders(): Flow<List<ElementEntity>>

    @Query("SELECT * FROM elements WHERE id = :id")
    suspend fun getById(id: Long): ElementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(element: ElementEntity): Long

    @Delete
    suspend fun delete(element: ElementEntity)

    @Query("DELETE FROM elements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
