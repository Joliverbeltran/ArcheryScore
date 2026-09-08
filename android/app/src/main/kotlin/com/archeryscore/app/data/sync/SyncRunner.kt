package com.archeryscore.app.data.sync

import com.archeryscore.app.data.local.dao.SyncWriteDao
import com.archeryscore.app.data.local.entity.SyncWriteEntity
import java.io.IOException
import java.time.Instant

class SyncRunner(
    private val remote: SessionRemoteDataSource,
    private val outboxDao: SyncWriteDao,
    private val now: () -> Instant = Instant::now,
    private val maxAttempts: Int = 10,
) {

    suspend fun syncForUser(userId: String): SyncResult {
        var pushed = 0
        var failed = 0
        try {
            val batch = outboxDao.pending(userId, BATCH_SIZE)
            for (write in batch) {
                if (write.attempts >= maxAttempts) {
                    // Saturated write: stop retrying, keep it visible as PENDING/ERROR.
                    failed++
                    continue
                }
                if (tryPush(write)) {
                    outboxDao.deleteById(write.id)
                    pushed++
                } else {
                    failed++
                }
            }
        } catch (t: Throwable) {
            if (t is IOException) {
                return SyncResult.Partial(pushed, failed, retryable = true)
            }
            throw t
        }
        return SyncResult.Full(pushed, failed)
    }

    private suspend fun tryPush(write: SyncWriteEntity): Boolean {
        return try {
            when {
                write.operation == "DELETE" && write.entityType == "session" ->
                    remote.deleteSession(PayloadCodec.decodeDeletion(write.payload))
                write.entityType == "session" ->
                    remote.pushSession(PayloadCodec.decodeSessionV1(write.payload))
                write.entityType == "end" ->
                    remote.pushEnd(PayloadCodec.decodeEndV1(write.payload))
                write.entityType == "arrow" ->
                    remote.pushArrow(PayloadCodec.decodeArrowV1(write.payload))
                else -> error("Unknown outbox write: ${write.operation}/${write.entityType}")
            }
            true
        } catch (t: Throwable) {
            outboxDao.bumpAttempts(write.id)
            false
        }
    }

    sealed interface SyncResult {
        data class Full(val pushed: Int, val failed: Int) : SyncResult
        data class Partial(val pushed: Int, val failed: Int, val retryable: Boolean) : SyncResult
    }

    companion object {
        private const val BATCH_SIZE = 100
    }
}