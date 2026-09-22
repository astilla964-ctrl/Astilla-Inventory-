package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_logs",
    indices = [Index("productId")]
)
data class StockLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val changeAmount: Int,
    val resultingStock: Int,
    val reason: String, // "SALE", "RESTOCK", "INITIAL_STOCK", "ADJUSTMENT"
    val timestamp: Long = System.currentTimeMillis()
)
