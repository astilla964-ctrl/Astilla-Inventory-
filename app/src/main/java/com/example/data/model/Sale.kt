package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String = "CASH", // CASH, CARD, DIGITAL_WALLET
    val customerNotes: String = ""
)
