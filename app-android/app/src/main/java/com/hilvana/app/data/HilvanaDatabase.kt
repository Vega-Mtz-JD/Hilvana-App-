package com.hilvana.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Item::class, Folder::class], version = 5, exportSchema = false)
abstract class HilvanaDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun folderDao(): FolderDao

    companion object {
        fun create(context: Context): HilvanaDatabase =
            Room.databaseBuilder(context, HilvanaDatabase::class.java, "hilvana.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(SeedFolders)
                .build()

        private fun insertIdeasFolder(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO folders (name, emoji, createdAt) VALUES (?, ?, ?)",
                arrayOf<Any>("Ideas", "💡", System.currentTimeMillis()),
            )
        }

        /**
         * v1 → v2: llegan las carpetas y se unifican Idea y Nota. Las ideas que ya
         * existían pasan a ser notas dentro de la carpeta "💡 Ideas".
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS folders (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, emoji TEXT NOT NULL, createdAt INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE items ADD COLUMN folderId INTEGER")
                insertIdeasFolder(db)
                db.execSQL(
                    "UPDATE items SET type = 'NOTE', " +
                        "folderId = (SELECT id FROM folders WHERE name = 'Ideas') WHERE type = 'IDEA'"
                )
            }
        }

        /** v2 → v3: imagen adjunta en las notas. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN imagePath TEXT")
            }
        }

        /** v3 → v4: carpetas protegidas con huella/PIN. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN locked INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v4 → v5: importancia (estrellas) y fecha de completado (estadísticas). */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN completedAt INTEGER")
            }
        }

        /** Instalación nueva: empieza con una carpeta de ejemplo. */
        private object SeedFolders : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                insertIdeasFolder(db)
            }
        }
    }
}
