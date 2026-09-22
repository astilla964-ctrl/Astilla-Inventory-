package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StockLog
import com.example.data.model.SyncQueueItem
import com.example.data.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Product::class, Sale::class, SaleItem::class, StockLog::class, SyncQueueItem::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pos_inventory_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with realistic retail products & sample sales
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).seedInitialData()
                        }
                    }
                })
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialData() {
        val initialProducts = listOf(
            Product(
                barcode = "8901030383124",
                name = "Organic Espresso Beans (500g)",
                category = "Coffee & Tea",
                costPrice = 8.50,
                sellingPrice = 16.99,
                stockQuantity = 24,
                lowStockThreshold = 8,
                unit = "bag"
            ),
            Product(
                barcode = "7622210449283",
                name = "Dark Chocolate Almond Bar (85%)",
                category = "Snacks",
                costPrice = 1.60,
                sellingPrice = 3.99,
                stockQuantity = 4, // Trigger low stock alert!
                lowStockThreshold = 10,
                unit = "bar"
            ),
            Product(
                barcode = "012000001291",
                name = "Cold Brew Sparkling Tonic",
                category = "Beverages",
                costPrice = 1.25,
                sellingPrice = 3.49,
                stockQuantity = 18,
                lowStockThreshold = 6,
                unit = "can"
            ),
            Product(
                barcode = "041220576321",
                name = "Artisan Sourdough Loaf",
                category = "Bakery",
                costPrice = 2.40,
                sellingPrice = 5.75,
                stockQuantity = 3, // Low stock alert!
                lowStockThreshold = 5,
                unit = "loaf"
            ),
            Product(
                barcode = "5000112548167",
                name = "Ceramic Pour-Over Mug",
                category = "Merchandise",
                costPrice = 7.00,
                sellingPrice = 18.50,
                stockQuantity = 12,
                lowStockThreshold = 4,
                unit = "pcs"
            ),
            Product(
                barcode = "8901234567890",
                name = "Oat Milk Barista Edition (1L)",
                category = "Dairy & Plant Milk",
                costPrice = 2.10,
                sellingPrice = 4.79,
                stockQuantity = 2, // Low stock alert!
                lowStockThreshold = 8,
                unit = "carton"
            ),
            Product(
                barcode = "070847811169",
                name = "Matcha Green Tea Latte Powder",
                category = "Coffee & Tea",
                costPrice = 9.20,
                sellingPrice = 19.99,
                stockQuantity = 15,
                lowStockThreshold = 5,
                unit = "tin"
            ),
            Product(
                barcode = "028400040112",
                name = "Gourmet Sea Salt Kettle Chips",
                category = "Snacks",
                costPrice = 0.90,
                sellingPrice = 2.49,
                stockQuantity = 32,
                lowStockThreshold = 12,
                unit = "bag"
            )
        )

        productDao().insertAll(initialProducts)

        // Seed some historical sales for dashboard trends (over the last 7 days)
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        val dummySales = listOf(
            Triple(now - 6 * dayMillis, 142.50, "CASH"),
            Triple(now - 5 * dayMillis, 215.80, "CARD"),
            Triple(now - 4 * dayMillis, 189.40, "CARD"),
            Triple(now - 3 * dayMillis, 310.20, "DIGITAL_WALLET"),
            Triple(now - 2 * dayMillis, 268.90, "CARD"),
            Triple(now - 1 * dayMillis, 345.60, "CASH"),
            Triple(now - 2 * 60 * 60 * 1000L, 88.20, "CARD")
        )

        dummySales.forEachIndexed { index, (time, amount, method) ->
            val saleId = saleDao().insertSale(
                Sale(
                    saleNumber = "POS-DEMO-100${index + 1}",
                    timestamp = time,
                    subtotal = amount,
                    discount = 0.0,
                    tax = amount * 0.08,
                    totalAmount = amount * 1.08,
                    paymentMethod = method,
                    customerNotes = "Initial demo receipt"
                )
            )

            // Add corresponding sale items
            saleDao().insertSaleItems(
                listOf(
                    SaleItem(
                        saleId = saleId,
                        productId = 1L,
                        barcode = "8901030383124",
                        productName = "Organic Espresso Beans (500g)",
                        quantity = 2,
                        unitPrice = 16.99,
                        costPrice = 8.50,
                        subtotal = 33.98
                    ),
                    SaleItem(
                        saleId = saleId,
                        productId = 3L,
                        barcode = "012000001291",
                        productName = "Cold Brew Sparkling Tonic",
                        quantity = 4,
                        unitPrice = 3.49,
                        costPrice = 1.25,
                        subtotal = 13.96
                    )
                )
            )
        }
    }
}
