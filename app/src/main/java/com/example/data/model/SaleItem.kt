package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val barcode: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val costPrice: Double,
    val subtotal: Double
)
