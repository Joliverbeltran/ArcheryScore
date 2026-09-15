package com.archeryscore.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.archeryscore.app.data.local.AppDatabase
import com.archeryscore.app.data.local.dao.ArrowDao
import com.archeryscore.app.data.local.dao.EndDao
import com.archeryscore.app.data.local.dao.SessionDao
import com.archeryscore.app.data.prefs.DataStorePreferencesRepository
import com.archeryscore.app.data.repository.DefaultStatsRepository
import com.archeryscore.app.data.repository.RoomSessionRepository
import com.archeryscore.app.domain.repository.PreferencesRepository
import com.archeryscore.app.domain.repository.SessionRepository
import com.archeryscore.app.domain.repository.StatsRepository
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideEndDao(db: AppDatabase): EndDao = db.endDao()

    @Provides
    fun provideArrowDao(db: AppDatabase): ArrowDao = db.arrowDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            produceFile = {
                java.io.File(context.filesDir, "datastore/prefs.preferences_pb")
            }
        )

    @Provides
    @Singleton
    fun provideDataStorePreferencesRepository(store: DataStore<Preferences>) =
        DataStorePreferencesRepository(store)

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        endDao: EndDao,
        arrowDao: ArrowDao,
    ): SessionRepository = RoomSessionRepository(sessionDao, endDao, arrowDao)

    @Provides
    @Singleton
    fun providePreferencesRepository(prefs: DataStorePreferencesRepository): PreferencesRepository =
        prefs

    @Provides
    @Singleton
    fun provideStatsRepository(sessionRepository: SessionRepository): StatsRepository =
        DefaultStatsRepository(sessionRepository)
}