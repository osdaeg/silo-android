package com.daniel.silo.di

import android.content.Context
import androidx.room.Room
import com.daniel.silo.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SiloDatabase =
        Room.databaseBuilder(ctx, SiloDatabase::class.java, "silo.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideLinkDao(db: SiloDatabase): LinkDao = db.linkDao()
    @Provides fun provideCollectionDao(db: SiloDatabase): CollectionDao = db.collectionDao()
}
