package com.hilvana.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.text.Normalizer

/** Carpeta personalizada de notas: "💡 Ideas", "🔗 Links", "🔑 Claves"… */
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = DEFAULT_EMOJI,
    val createdAt: Long = System.currentTimeMillis(),
    /** Protegida: sus notas se ocultan hasta desbloquear con huella o PIN. */
    @ColumnInfo(defaultValue = "0") val locked: Boolean = false,
) {
    val label: String get() = "$emoji $name" + if (locked) " 🔒" else ""

    /** "¿Esta carpeta se llama así?" sin importar mayúsculas ni acentos. */
    fun matches(name: String): Boolean = normalize(this.name) == normalize(name)

    companion object {
        const val DEFAULT_EMOJI = "📁"

        private fun normalize(text: String): String =
            Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
    }
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt")
    fun observeAll(): Flow<List<Folder>>

    @Insert
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)
}
