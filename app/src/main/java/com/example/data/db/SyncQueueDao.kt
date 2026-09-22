package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC")
    fun getAllQueueItems(): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    fun getPendingQueueItems(): Flow<List<SyncQueueItem>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPendingItemsSync(): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getQueueItemById(id: Long): SyncQueueItem?

    @Query("SELECT * FROM sync_queue WHERE saleId = :saleId LIMIT 1")
    suspend fun getQueueItemBySaleId(saleId: Long): SyncQueueItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueItem): Long

    @Update
    suspend fun update(item: SyncQueueItem)

    @Query("UPDATE sync_queue SET status = :status, lastAttemptAt = :time WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'SYNCED', lastAttemptAt = :time, lastError = null WHERE id = :id")
    suspend fun markSynced(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'FAILED', lastAttemptAt = :time, lastError = :error, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: Long, error: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'PENDING', lastError = null WHERE status = 'FAILED'")
    suspend fun retryAllFailed()

    @Delete
    suspend fun delete(item: SyncQueueItem)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearCompleted()
}
