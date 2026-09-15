package com.archeryscore.app.data.sync

import com.archeryscore.app.data.local.dao.SyncWriteDao
import com.archeryscore.app.data.local.entity.SyncWriteEntity

class SyncOutboxWriter(private val dao: SyncWriteDao) {

    suspend fun enqueue(
        userId: String,
        entityType: String,
        entityId: String,
        payload: String,
        operation: String = "UPSERT",
    ) {
        dao.insert(
            SyncWriteEntity(
                userId = userId,
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                createdAt = System.currentTimeMillis(),
                attempts = 0,
            )
        )
    }
}