package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.GoogleSheetsBackupService
import com.example.data.db.AppDatabase
import com.example.data.db.TopProductStat
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.StockLog
import com.example.data.model.SyncQueueItem
import com.example.data.model.SyncStatus
import com.example.data.network.NetworkMonitor
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppTab {
    POS,
    INVENTORY,
    DASHBOARD,
    BACKUP
}

enum class ScannerMode {
    SCAN_TO_CART,
    SCAN_TO_INVENTORY
}

enum class TimeframeFilter {
    TODAY,
    WEEK,
    MONTH,
    ALL
}

data class TrendPoint(
    val label: String,
    val timestamp: Long,
    val revenue: Double,
    val transactions: Int
)

data class DashboardMetrics(
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val transactionCount: Int = 0,
    val avgTicket: Double = 0.0,
    val unitsSold: Int = 0,
    val trendPoints: List<TrendPoint> = emptyList(),
    val topProducts: List<TopProductStat> = emptyList(),
    val categoryBreakdown: Map<String, Double> = emptyMap()
)

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = PosRepository(database)
    val backupService = GoogleSheetsBackupService(application, repository)
    val networkMonitor = NetworkMonitor(application)

    // Network connectivity status
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.isOnline())

    // Local Room Synchronization Queue
    val allSyncQueue: StateFlow<List<SyncQueueItem>> = repository.allSyncQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncQueue: StateFlow<List<SyncQueueItem>> = repository.pendingSyncQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(AppTab.POS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Products & Categories
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topSellingProducts: StateFlow<List<TopProductStat>> = repository.topSellingProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStockLogs: StateFlow<List<StockLog>> = repository.recentStockLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // POS Register State
    private val _posSearchQuery = MutableStateFlow("")
    val posSearchQuery = _posSearchQuery.asStateFlow()

    private val _posSelectedCategory = MutableStateFlow<String?>(null)
    val posSelectedCategory = _posSelectedCategory.asStateFlow()

    private val _cartItems = MutableStateFlow<List<PosRepository.CartItem>>(emptyList())
    val cartItems = _cartItems.asStateFlow()

    private val _discountPercent = MutableStateFlow(0.0)
    val discountPercent = _discountPercent.asStateFlow()

    private val _taxPercent = MutableStateFlow(8.0) // Standard default 8% sales tax
    val taxPercent = _taxPercent.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow("CASH")
    val selectedPaymentMethod = _selectedPaymentMethod.asStateFlow()

    private val _cashTendered = MutableStateFlow("")
    val cashTendered = _cashTendered.asStateFlow()

    private val _lastCompletedReceipt = MutableStateFlow<PosRepository.CheckoutResult?>(null)
    val lastCompletedReceipt = _lastCompletedReceipt.asStateFlow()

    // Inventory State
    private val _inventorySearchQuery = MutableStateFlow("")
    val inventorySearchQuery = _inventorySearchQuery.asStateFlow()

    private val _inventoryFilterLowStockOnly = MutableStateFlow(false)
    val inventoryFilterLowStockOnly = _inventoryFilterLowStockOnly.asStateFlow()

    private val _inventorySelectedCategory = MutableStateFlow<String?>(null)
    val inventorySelectedCategory = _inventorySelectedCategory.asStateFlow()

    // Scanner State
    private val _isScannerOpen = MutableStateFlow(false)
    val isScannerOpen = _isScannerOpen.asStateFlow()

    private val _scannerMode = MutableStateFlow(ScannerMode.SCAN_TO_CART)
    val scannerMode = _scannerMode.asStateFlow()

    private val _scannedProductForInventory = MutableStateFlow<Product?>(null)
    val scannedProductForInventory = _scannedProductForInventory.asStateFlow()

    private val _scannedNewBarcode = MutableStateFlow<String?>(null)
    val scannedNewBarcode = _scannedNewBarcode.asStateFlow()

    // Dashboard Timeframe State
    private val _timeframeFilter = MutableStateFlow(TimeframeFilter.WEEK)
    val timeframeFilter = _timeframeFilter.asStateFlow()

    // Backup State
    private val _webhookUrlInput = MutableStateFlow(backupService.webhookUrl)
    val webhookUrlInput = _webhookUrlInput.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(backupService.autoBackupOnSale)
    val autoBackupEnabled = _autoBackupEnabled.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp = _isBackingUp.asStateFlow()

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage = _backupStatusMessage.asStateFlow()

    // Toast / UI Notification events
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            var wasOffline = false
            networkMonitor.isOnlineFlow.collect { online ->
                if (online && wasOffline) {
                    val pending = repository.getPendingSyncItems()
                    if (pending.isNotEmpty() && backupService.webhookUrl.isNotBlank()) {
                        _uiEvents.emit("⚡ Connection restored! Automatically syncing ${pending.size} offline sale(s) to Google Sheets...")
                        processSyncQueueInternal()
                    }
                }
                wasOffline = !online
            }
        }
    }

    // Filtered POS products
    val filteredPosProducts: StateFlow<List<Product>> = combine(
        allProducts,
        posSearchQuery,
        posSelectedCategory
    ) { products, query, cat ->
        products.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true) ||
                    p.category.contains(query, ignoreCase = true)
            val matchesCat = cat == null || p.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Inventory products
    val filteredInventoryProducts: StateFlow<List<Product>> = combine(
        allProducts,
        inventorySearchQuery,
        inventoryFilterLowStockOnly,
        inventorySelectedCategory
    ) { products, query, lowStockOnly, cat ->
        products.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true) ||
                    p.category.contains(query, ignoreCase = true)
            val matchesLowStock = !lowStockOnly || p.isLowStock
            val matchesCat = cat == null || p.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesLowStock && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard metrics calculation
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allSales,
        allProducts,
        timeframeFilter,
        topSellingProducts
    ) { sales, products, timeframe, topProds ->
        calculateDashboardMetrics(sales, products, timeframe, topProds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- POS Actions ---
    fun setPosSearch(query: String) {
        _posSearchQuery.value = query
    }

    fun setPosCategory(category: String?) {
        _posSelectedCategory.value = category
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        if (product.stockQuantity <= 0) {
            viewModelScope.launch {
                _uiEvents.emit("${product.name} is currently out of stock!")
            }
            return
        }

        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + quantity
            if (newQty > product.stockQuantity) {
                viewModelScope.launch {
                    _uiEvents.emit("Cannot exceed available stock (${product.stockQuantity} ${product.unit})")
                }
                return
            }
            current[index] = existing.copy(quantity = newQty)
        } else {
            current.add(PosRepository.CartItem(product, quantity))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: Long, quantity: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (quantity <= 0) {
                current.removeAt(index)
            } else {
                val item = current[index]
                if (quantity > item.product.stockQuantity) {
                    viewModelScope.launch {
                        _uiEvents.emit("Max stock available is ${item.product.stockQuantity}")
                    }
                    current[index] = item.copy(quantity = item.product.stockQuantity)
                } else {
                    current[index] = item.copy(quantity = quantity)
                }
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _discountPercent.value = 0.0
        _cashTendered.value = ""
    }

    fun setDiscount(percent: Double) {
        _discountPercent.value = percent.coerceIn(0.0, 100.0)
    }

    fun setTax(percent: Double) {
        _taxPercent.value = percent.coerceAtLeast(0.0)
    }

    fun setPaymentMethod(method: String) {
        _selectedPaymentMethod.value = method
    }

    fun setCashTendered(amount: String) {
        _cashTendered.value = amount
    }

    fun completeCheckout(notes: String = "") {
        val cart = _cartItems.value
        if (cart.isEmpty()) return

        viewModelScope.launch {
            try {
                val result = repository.processSale(
                    cartItems = cart,
                    paymentMethod = _selectedPaymentMethod.value,
                    discountPercent = _discountPercent.value,
                    taxPercent = _taxPercent.value,
                    notes = notes
                )

                _lastCompletedReceipt.value = result
                clearCart()

                var msg = "Sale #${result.saleNumber} completed! Total: $${String.format(Locale.US, "%.2f", result.totalAmount)}"
                if (result.lowStockAlertsTriggered.isNotEmpty()) {
                    msg += " ⚠️ Alert: ${result.lowStockAlertsTriggered.size} items now low on stock!"
                }
                _uiEvents.emit(msg)

                // If currently offline, notify that sale was safely queued locally in Room
                if (!networkMonitor.isOnline()) {
                    _uiEvents.emit("📦 Offline Mode: Sale queued locally in Room. It will automatically push once connection is restored.")
                } else if (_autoBackupEnabled.value && backupService.webhookUrl.isNotBlank()) {
                    launch {
                        processSyncQueueInternal()
                    }
                }
            } catch (e: Exception) {
                _uiEvents.emit("Checkout error: ${e.localizedMessage}")
            }
        }
    }

    fun dismissReceipt() {
        _lastCompletedReceipt.value = null
    }

    // --- Barcode Scanner Handling ---
    fun openScanner(mode: ScannerMode) {
        _scannerMode.value = mode
        _isScannerOpen.value = true
    }

    fun closeScanner() {
        _isScannerOpen.value = false
        _scannedProductForInventory.value = null
        _scannedNewBarcode.value = null
    }

    fun handleBarcodeScanned(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) return

        viewModelScope.launch {
            val product = repository.getProductByBarcode(cleanBarcode)
            when (_scannerMode.value) {
                ScannerMode.SCAN_TO_CART -> {
                    if (product != null) {
                        addToCart(product, 1)
                        _uiEvents.emit("Scanned: ${product.name} (Added to cart)")
                    } else {
                        _uiEvents.emit("Product not found with barcode: $cleanBarcode")
                    }
                }
                ScannerMode.SCAN_TO_INVENTORY -> {
                    if (product != null) {
                        _scannedProductForInventory.value = product
                        _uiEvents.emit("Found existing product: ${product.name}")
                    } else {
                        _scannedNewBarcode.value = cleanBarcode
                        _uiEvents.emit("New barcode scanned: $cleanBarcode")
                    }
                    _isScannerOpen.value = false
                }
            }
        }
    }

    fun clearScannedInventoryItem() {
        _scannedProductForInventory.value = null
    }

    fun clearScannedNewBarcode() {
        _scannedNewBarcode.value = null
    }

    // --- Inventory Actions ---
    fun setInventorySearch(query: String) {
        _inventorySearchQuery.value = query
    }

    fun toggleInventoryLowStockOnly() {
        _inventoryFilterLowStockOnly.value = !_inventoryFilterLowStockOnly.value
    }

    fun setInventoryCategory(cat: String?) {
        _inventorySelectedCategory.value = cat
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            try {
                if (product.id == 0L) {
                    repository.insertProduct(product)
                    _uiEvents.emit("Product '${product.name}' added to inventory!")
                } else {
                    repository.updateProduct(product)
                    _uiEvents.emit("Product '${product.name}' updated successfully!")
                }
            } catch (e: Exception) {
                _uiEvents.emit("Error saving product: ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
                _uiEvents.emit("Deleted '${product.name}' from inventory.")
            } catch (e: Exception) {
                _uiEvents.emit("Error deleting product: ${e.localizedMessage}")
            }
        }
    }

    fun quickRestock(product: Product, quantityToAdd: Int) {
        if (quantityToAdd <= 0) return
        viewModelScope.launch {
            try {
                repository.restockProduct(product, quantityToAdd)
                _uiEvents.emit("Restocked ${product.name} (+ $quantityToAdd). New stock: ${product.stockQuantity + quantityToAdd}")
            } catch (e: Exception) {
                _uiEvents.emit("Error restocking: ${e.localizedMessage}")
            }
        }
    }

    // --- Dashboard Timeframe ---
    fun setTimeframe(filter: TimeframeFilter) {
        _timeframeFilter.value = filter
    }

    private fun calculateDashboardMetrics(
        allSales: List<Sale>,
        products: List<Product>,
        timeframe: TimeframeFilter,
        topProducts: List<TopProductStat>
    ): DashboardMetrics {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val startTime = when (timeframe) {
            TimeframeFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            TimeframeFilter.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            TimeframeFilter.MONTH -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.timeInMillis
            }
            TimeframeFilter.ALL -> 0L
        }

        val filteredSales = allSales.filter { it.timestamp >= startTime }
        val totalRevenue = filteredSales.sumOf { it.totalAmount }
        val totalRawSubtotal = filteredSales.sumOf { it.subtotal }
        // Approximate profit based on average margin across catalog (~40% fallback if items not joined)
        val estimatedProfit = totalRawSubtotal * 0.45
        val transactionCount = filteredSales.size
        val avgTicket = if (transactionCount > 0) totalRevenue / transactionCount else 0.0

        // Build Trend points (group by day)
        val sdf = SimpleDateFormat("MM/dd", Locale.US)
        val salesByDate = filteredSales.groupBy { sdf.format(Date(it.timestamp)) }
        val trendPoints = salesByDate.map { (dateLabel, salesInDay) ->
            TrendPoint(
                label = dateLabel,
                timestamp = salesInDay.firstOrNull()?.timestamp ?: 0L,
                revenue = salesInDay.sumOf { it.totalAmount },
                transactions = salesInDay.size
            )
        }.sortedBy { it.timestamp }

        // Category breakdown
        val categoryBreakdown = products.groupBy { it.category }
            .mapValues { (_, prods) -> prods.sumOf { it.sellingPrice * it.stockQuantity } }

        return DashboardMetrics(
            totalRevenue = totalRevenue,
            totalProfit = estimatedProfit,
            transactionCount = transactionCount,
            avgTicket = avgTicket,
            unitsSold = filteredSales.size * 2,
            trendPoints = trendPoints,
            topProducts = topProducts,
            categoryBreakdown = categoryBreakdown
        )
    }

    // --- Google Sheets Backup Actions ---
    fun setWebhookUrl(url: String) {
        _webhookUrlInput.value = url
        backupService.webhookUrl = url
    }

    fun toggleAutoBackup() {
        val newVal = !_autoBackupEnabled.value
        _autoBackupEnabled.value = newVal
        backupService.autoBackupOnSale = newVal
    }

    fun triggerGoogleSheetsSync() {
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupStatusMessage.value = "Connecting to Google Sheets..."
            val result = backupService.syncToGoogleSheets(_webhookUrlInput.value)
            _isBackingUp.value = false
            when (result) {
                is GoogleSheetsBackupService.BackupResult.Success -> {
                    _backupStatusMessage.value = result.message
                    _uiEvents.emit("Google Sheets sync completed!")
                    // Also clear pending queue items as full sync encompasses all sales
                    repository.clearCompletedSyncItems()
                }
                is GoogleSheetsBackupService.BackupResult.Error -> {
                    _backupStatusMessage.value = "Failed: ${result.message}"
                    _uiEvents.emit(result.message)
                }
            }
        }
    }

    // --- Offline Synchronization Queue Actions ---
    fun processSyncQueue() {
        viewModelScope.launch {
            processSyncQueueInternal()
        }
    }

    private suspend fun processSyncQueueInternal() {
        val pending = repository.getPendingSyncItems()
        if (pending.isEmpty()) {
            _backupStatusMessage.value = "Sync queue is clear — no pending offline sales."
            return
        }

        if (backupService.webhookUrl.isBlank()) {
            _backupStatusMessage.value = "Google Sheets Webhook URL is not configured. Set URL to sync."
            _uiEvents.emit("Google Sheets Webhook URL required to sync offline queue.")
            return
        }

        if (!networkMonitor.isOnline()) {
            _backupStatusMessage.value = "Device is currently offline. ${pending.size} sales preserved in local queue."
            _uiEvents.emit("Device is offline. Will automatically sync when connection returns.")
            return
        }

        _isBackingUp.value = true
        _backupStatusMessage.value = "Pushing ${pending.size} queued offline sale(s) to Google Sheets..."

        // Mark items as SYNCING in Room
        pending.forEach {
            repository.updateSyncItemStatus(it.id, SyncStatus.SYNCING.name)
        }

        val result = backupService.pushQueuedSales(pending, _webhookUrlInput.value)
        _isBackingUp.value = false

        when (result) {
            is GoogleSheetsBackupService.BackupResult.Success -> {
                pending.forEach {
                    repository.markSyncItemSynced(it.id)
                }
                _backupStatusMessage.value = "Successfully pushed ${pending.size} sale(s) to Google Sheets!"
                _uiEvents.emit("✓ Pushed ${pending.size} offline sale(s) to Google Sheets!")
            }
            is GoogleSheetsBackupService.BackupResult.Error -> {
                pending.forEach {
                    repository.markSyncItemFailed(it.id, result.message)
                }
                _backupStatusMessage.value = "Sync failed: ${result.message}"
                _uiEvents.emit("Sync failed: ${result.message}")
            }
        }
    }

    fun retryFailedQueue() {
        viewModelScope.launch {
            repository.retryAllFailedSyncItems()
            processSyncQueueInternal()
        }
    }

    fun clearCompletedQueue() {
        viewModelScope.launch {
            repository.clearCompletedSyncItems()
            _uiEvents.emit("Cleared synced items from queue.")
        }
    }

    fun deleteQueueItem(id: Long) {
        viewModelScope.launch {
            repository.deleteSyncItem(id)
            _uiEvents.emit("Removed item from sync queue.")
        }
    }
}
