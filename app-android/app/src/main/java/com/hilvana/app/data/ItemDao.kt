package com.hilvana.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    suspend fun insert(item: Item): Long

    @Delete
    suspend fun delete(item: Item)

    /** La Bandeja: lo capturado sin ordenar, lo más reciente primero. */
    @Query("SELECT * FROM items WHERE inInbox = 1 ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): Item?

    /** Recordatorios que aún no suenan (para reprogramarlos al reiniciar el teléfono). */
    @Query("SELECT * FROM items WHERE dueAt IS NOT NULL AND dueAt > :now AND done = 0")
    suspend fun getUpcomingReminders(now: Long): List<Item>
}
