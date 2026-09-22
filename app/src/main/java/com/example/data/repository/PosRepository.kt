package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.TopProductStat
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StockLog
import com.example.data.model.SyncQueueItem
import com.example.data.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val saleDao = database.saleDao()
    private val syncQueueDao = database.syncQueueDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val categories: Flow<List<String>> = productDao.getAllCategories()
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val topSellingProducts: Flow<List<TopProductStat>> = saleDao.getTopSellingProducts(8)
    val recentStockLogs: Flow<List<StockLog>> = saleDao.getRecentStockLogs(40)
    val allSyncQueue: Flow<List<SyncQueueItem>> = syncQueueDao.getAllQueueItems()
    val pendingSyncQueue: Flow<List<SyncQueueItem>> = syncQueueDao.getPendingQueueItems()
    val pendingSyncCount: Flow<Int> = syncQueueDao.getPendingCount()

    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)

    suspend fun getProductByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        productDao.findProductByBarcodeSync(barcode.trim())
    }

    suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        val id = productDao.insertProduct(product)
        saleDao.insertStockLog(
            StockLog(
                productId = id,
                productName = product.name,
                changeAmount = product.stockQuantity,
                resultingStock = product.stockQuantity,
                reason = "INITIAL_STOCK"
            )
        )
        id
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun restockProduct(product: Product, addedQuantity: Int, note: String = "Restock") = withContext(Dispatchers.IO) {
        val newStock = product.stockQuantity + addedQuantity
        productDao.updateStock(product.id, newStock, System.currentTimeMillis())
        saleDao.insertStockLog(
            StockLog(
                productId = product.id,
                productName = product.name,
                changeAmount = addedQuantity,
                resultingStock = newStock,
                reason = "RESTOCK"
            )
        )
    }

    data class CartItem(
        val product: Product,
        val quantity: Int
    ) {
        val subtotal: Double get() = product.sellingPrice * quantity
    }

    data class CheckoutResult(
        val saleId: Long,
        val saleNumber: String,
        val totalAmount: Double,
        val lowStockAlertsTriggered: List<Product>,
        val queueItemId: Long = 0L
    )

    suspend fun processSale(
        cartItems: List<CartItem>,
        paymentMethod: String,
        discountPercent: Double = 0.0,
        taxPercent: Double = 0.0,
        notes: String = ""
    ): CheckoutResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyMMdd-HHmmss", Locale.US)
        val saleNumber = "SALE-${sdf.format(Date(now))}"

        val rawSubtotal = cartItems.sumOf { it.subtotal }
        val discount = rawSubtotal * (discountPercent / 100.0)
        val discountedSubtotal = rawSubtotal - discount
        val tax = discountedSubtotal * (taxPercent / 100.0)
        val totalAmount = discountedSubtotal + tax

        val sale = Sale(
            saleNumber = saleNumber,
            timestamp = now,
            subtotal = rawSubtotal,
            discount = discount,
            tax = tax,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            customerNotes = notes
        )

        val saleId = saleDao.insertSale(sale)

        val saleItems = cartItems.map { cartItem ->
            SaleItem(
                saleId = saleId,
                productId = cartItem.product.id,
                barcode = cartItem.product.barcode,
                productName = cartItem.product.name,
                quantity = cartItem.quantity,
                unitPrice = cartItem.product.sellingPrice,
                costPrice = cartItem.product.costPrice,
                subtotal = cartItem.subtotal
            )
        }
        saleDao.insertSaleItems(saleItems)

        val lowStockAlerts = mutableListOf<Product>()

        // Update inventory & write stock logs
        cartItems.forEach { cartItem ->
            val newStock = (cartItem.product.stockQuantity - cartItem.quantity).coerceAtLeast(0)
            productDao.updateStock(cartItem.product.id, newStock, now)

            saleDao.insertStockLog(
                StockLog(
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    changeAmount = -cartItem.quantity,
                    resultingStock = newStock,
                    reason = "SALE"
                )
            )

            if (newStock <= cartItem.product.lowStockThreshold) {
                lowStockAlerts.add(cartItem.product.copy(stockQuantity = newStock))
            }
        }

        // Enqueue into local Room synchronization queue
        val payloadObj = JSONObject().apply {
            put("saleId", saleId)
            put("saleNumber", saleNumber)
            put("timestamp", now)
            put("subtotal", rawSubtotal)
            put("discount", discount)
            put("tax", tax)
            put("totalAmount", totalAmount)
            put("paymentMethod", paymentMethod)
            put("customerNotes", notes)
            val itemsJson = JSONArray()
            cartItems.forEach { item ->
                itemsJson.put(JSONObject().apply {
                    put("barcode", item.product.barcode)
                    put("name", item.product.name)
                    put("quantity", item.quantity)
                    put("unitPrice", item.product.sellingPrice)
                    put("costPrice", item.product.costPrice)
                    put("subtotal", item.subtotal)
                })
            }
            put("items", itemsJson)
        }

        val queueItemId = syncQueueDao.insert(
            SyncQueueItem(
                saleId = saleId,
                saleNumber = saleNumber,
                totalAmount = totalAmount,
                itemCount = cartItems.sumOf { it.quantity },
                paymentMethod = paymentMethod,
                createdAt = now,
                status = SyncStatus.PENDING.name,
                payloadJson = payloadObj.toString()
            )
        )

        CheckoutResult(
            saleId = saleId,
            saleNumber = saleNumber,
            totalAmount = totalAmount,
            lowStockAlertsTriggered = lowStockAlerts,
            queueItemId = queueItemId
        )
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItem> = withContext(Dispatchers.IO) {
        saleDao.getSaleItemsForSale(saleId)
    }

    suspend fun getAllProductsSync(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getAllProducts().first()
    }

    suspend fun getAllSalesSync(): List<Sale> = withContext(Dispatchers.IO) {
        saleDao.getAllSales().first()
    }

    suspend fun getLowStockProductsSync(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getLowStockProducts().first()
    }

    // Sync Queue Management
    suspend fun getPendingSyncItems(): List<SyncQueueItem> = withContext(Dispatchers.IO) {
        syncQueueDao.getPendingItemsSync()
    }

    suspend fun markSyncItemSynced(id: Long) = withContext(Dispatchers.IO) {
        syncQueueDao.markSynced(id)
    }

    suspend fun markSyncItemFailed(id: Long, error: String) = withContext(Dispatchers.IO) {
        syncQueueDao.markFailed(id, error)
    }

    suspend fun updateSyncItemStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        syncQueueDao.updateStatus(id, status)
    }

    suspend fun retryAllFailedSyncItems() = withContext(Dispatchers.IO) {
        syncQueueDao.retryAllFailed()
    }

    suspend fun clearCompletedSyncItems() = withContext(Dispatchers.IO) {
        syncQueueDao.clearCompleted()
    }

    suspend fun deleteSyncItem(id: Long) = withContext(Dispatchers.IO) {
        syncQueueDao.deleteById(id)
    }

    suspend fun getSyncQueueItemBySaleId(saleId: Long): SyncQueueItem? = withContext(Dispatchers.IO) {
        syncQueueDao.getQueueItemBySaleId(saleId)
    }
}
