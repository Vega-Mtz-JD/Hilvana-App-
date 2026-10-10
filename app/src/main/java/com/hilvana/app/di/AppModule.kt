package com.hilvana.app.di

import android.content.Context
import androidx.room.Room
import com.hilvana.app.data.local.HilvanaDatabase
import com.hilvana.app.data.local.dao.ElementDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HilvanaDatabase =
        Room.databaseBuilder(context, HilvanaDatabase::class.java, "hilvana.db")
            .build()

    @Provides
    fun provideElementDao(db: HilvanaDatabase): ElementDao = db.elementDao()
}
