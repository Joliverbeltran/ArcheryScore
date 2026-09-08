package com.archeryscore.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.archeryscore.app.BuildConfig
import com.archeryscore.app.data.auth.DefaultAuthRepository
import com.archeryscore.app.data.local.AppDatabase
import com.archeryscore.app.data.local.dao.ArrowDao
import com.archeryscore.app.data.local.dao.EndDao
import com.archeryscore.app.data.local.dao.PreferencesDao
import com.archeryscore.app.data.local.dao.SessionDao
import com.archeryscore.app.data.local.dao.SyncWriteDao
import com.archeryscore.app.data.prefs.DataStorePreferencesRepository
import com.archeryscore.app.data.repository.RoomSessionRepository
import com.archeryscore.app.data.sync.DisabledRemoteDataSource
import com.archeryscore.app.data.sync.OutboxSyncStatusRepository
import com.archeryscore.app.data.sync.SessionRemoteDataSource
import com.archeryscore.app.data.sync.SupabaseRemoteDataSource
import com.archeryscore.app.data.sync.SyncOutboxWriter
import com.archeryscore.app.data.sync.SyncRunner
import com.archeryscore.app.domain.repository.AuthRepository
import com.archeryscore.app.domain.repository.PreferencesRepository
import com.archeryscore.app.domain.repository.SessionRepository
import com.archeryscore.app.domain.repository.SyncStatusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
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
    fun provideSyncWriteDao(db: AppDatabase): SyncWriteDao = db.syncWriteDao()

    @Provides
    fun providePreferencesDao(db: AppDatabase): PreferencesDao = db.preferencesDao()

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
    fun provideAuthRepository(prefs: DataStorePreferencesRepository): AuthRepository =
        DefaultAuthRepository(prefs)

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        endDao: EndDao,
        arrowDao: ArrowDao,
        syncWriteDao: SyncWriteDao,
        outbox: SyncOutboxWriter,
    ): SessionRepository = RoomSessionRepository(sessionDao, endDao, arrowDao, syncWriteDao, outbox)

    @Provides
    @Singleton
    fun provideOutboxWriter(dao: SyncWriteDao): SyncOutboxWriter = SyncOutboxWriter(dao)

    @Provides
    @Singleton
    fun provideRemote(): SessionRemoteDataSource {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) return DisabledRemoteDataSource()
        val client = createSupabaseClient(url, key) {
            install(Postgrest)
        }
        return SupabaseRemoteDataSource(client)
    }

    @Provides
    @Singleton
    fun provideSyncRunner(
        remote: SessionRemoteDataSource,
        dao: SyncWriteDao,
    ): SyncRunner = SyncRunner(remote, dao)

    @Provides
    @Singleton
    fun provideSyncStatusRepository(dao: SyncWriteDao): SyncStatusRepository =
        OutboxSyncStatusRepository(dao)

    @Provides
    @Singleton
    fun provideSyncScheduler(): com.archeryscore.app.data.sync.SyncScheduler =
        com.archeryscore.app.data.sync.SyncScheduler()

    @Provides
    @Singleton
    fun providePreferencesRepository(prefs: DataStorePreferencesRepository): PreferencesRepository =
        prefs
}