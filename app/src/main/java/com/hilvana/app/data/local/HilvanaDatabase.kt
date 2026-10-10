package com.hilvana.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hilvana.app.data.local.dao.ElementDao
import com.hilvana.app.data.local.entity.ElementEntity

@Database(
    entities = [ElementEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HilvanaDatabase : RoomDatabase() {
    abstract fun elementDao(): ElementDao
}
