package com.archeryscore.app.data.sync

import com.archeryscore.app.data.local.dao.SyncWriteDao
import com.archeryscore.app.domain.model.SyncStatus
import com.archeryscore.app.domain.repository.SyncStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OutboxSyncStatusRepository(
    private val syncWriteDao: SyncWriteDao,
) : SyncStatusRepository {

    override fun observeStatus(userId: String): Flow<SyncStatus> =
        syncWriteDao.observePending(userId).map { pending ->
            if (pending.isEmpty()) SyncStatus.SYNCED else SyncStatus.PENDING
        }
}