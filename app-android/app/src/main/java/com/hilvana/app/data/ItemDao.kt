package com.hilvana.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    /** Todos los elementos, lo más reciente primero. Cada pantalla filtra lo suyo. */
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Item>>

    /** Al borrar una carpeta sus notas no se pierden: quedan sin carpeta. */
    @Query("UPDATE items SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: Long)

    @Query("SELECT * FROM items ORDER BY createdAt")
    suspend fun getAll(): List<Item>

    /** Tareas en foco pendientes (las más importantes primero). */
    @Query("SELECT * FROM items WHERE pinned = 1 AND done = 0 ORDER BY priority DESC, createdAt")
    suspend fun getPinned(): List<Item>

    @Query("SELECT * FROM items WHERE pinned = 1 AND done = 0 ORDER BY priority DESC, createdAt")
    fun observePinned(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): Item?

    /** Recordatorios que aún no suenan (para reprogramarlos al reiniciar el teléfono). */
    @Query("SELECT * FROM items WHERE dueAt IS NOT NULL AND dueAt > :now AND done = 0")
    suspend fun getUpcomingReminders(now: Long): List<Item>
}
