package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StockLog
import kotlinx.coroutines.flow.Flow

data class SaleWithItems(
    val sale: Sale,
    val items: List<SaleItem>
)

data class TopProductStat(
    val productName: String,
    val barcode: String,
    val totalQuantitySold: Int,
    val totalRevenue: Double
)

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getSalesSince(startTime: Long): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsForSale(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items ORDER BY id DESC")
    fun getAllSaleItems(): Flow<List<SaleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockLog(log: StockLog)

    @Query("SELECT * FROM stock_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentStockLogs(limit: Int = 50): Flow<List<StockLog>>

    @Query("""
        SELECT productName, barcode, SUM(quantity) as totalQuantitySold, SUM(subtotal) as totalRevenue
        FROM sale_items
        GROUP BY barcode
        ORDER BY totalQuantitySold DESC
        LIMIT :limit
    """)
    fun getTopSellingProducts(limit: Int = 5): Flow<List<TopProductStat>>

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getSalesCount(): Int
}
