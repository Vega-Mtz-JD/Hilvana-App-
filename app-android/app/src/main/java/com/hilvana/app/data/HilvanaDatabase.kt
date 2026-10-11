package com.hilvana.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Item::class], version = 1, exportSchema = false)
abstract class HilvanaDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        fun create(context: Context): HilvanaDatabase =
            Room.databaseBuilder(context, HilvanaDatabase::class.java, "hilvana.db").build()
    }
}
