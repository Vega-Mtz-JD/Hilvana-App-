package com.hilvana.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Item::class, Folder::class], version = 2, exportSchema = false)
abstract class HilvanaDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun folderDao(): FolderDao

    companion object {
        fun create(context: Context): HilvanaDatabase =
            Room.databaseBuilder(context, HilvanaDatabase::class.java, "hilvana.db")
                .addMigrations(MIGRATION_1_2)
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

        /** Instalación nueva: empieza con una carpeta de ejemplo. */
        private object SeedFolders : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                insertIdeasFolder(db)
            }
        }
    }
}
