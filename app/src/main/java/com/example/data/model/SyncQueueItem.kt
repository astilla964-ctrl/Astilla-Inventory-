package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val saleNumber: String,
    val totalAmount: Double,
    val itemCount: Int,
    val paymentMethod: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = SyncStatus.PENDING.name,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val lastAttemptAt: Long? = null,
    val payloadJson: String = ""
) {
    val syncStatus: SyncStatus
        get() = try {
            SyncStatus.valueOf(status)
        } catch (_: Exception) {
            SyncStatus.PENDING
        }

    val isPending: Boolean get() = syncStatus == SyncStatus.PENDING
    val isSynced: Boolean get() = syncStatus == SyncStatus.SYNCED
    val isFailed: Boolean get() = syncStatus == SyncStatus.FAILED
    val isSyncing: Boolean get() = syncStatus == SyncStatus.SYNCING
}
