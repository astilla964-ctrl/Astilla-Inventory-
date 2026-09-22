package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val name: String,
    val category: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Int,
    val lowStockThreshold: Int = 5,
    val unit: String = "pcs",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stockQuantity <= lowStockThreshold

    val isOutOfStock: Boolean
        get() = stockQuantity <= 0

    val profitMargin: Double
        get() = if (sellingPrice > 0) ((sellingPrice - costPrice) / sellingPrice) * 100.0 else 0.0
}
