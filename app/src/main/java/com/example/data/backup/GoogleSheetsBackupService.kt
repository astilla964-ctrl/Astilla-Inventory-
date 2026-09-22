package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SyncQueueItem
import com.example.data.repository.PosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleSheetsBackupService(
    private val context: Context,
    private val repository: PosRepository
) {
    private val prefs = context.getSharedPreferences("google_sheets_backup_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    var webhookUrl: String
        get() = prefs.getString("webhook_url", "") ?: ""
        set(value) = prefs.edit().putString("webhook_url", value.trim()).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong("last_backup_time", 0L)
        set(value) = prefs.edit().putLong("last_backup_time", value).apply()

    var autoBackupOnSale: Boolean
        get() = prefs.getBoolean("auto_backup_on_sale", false)
        set(value) = prefs.edit().putBoolean("auto_backup_on_sale", value).apply()

    sealed class BackupResult {
        data class Success(val message: String, val productCount: Int, val salesCount: Int) : BackupResult()
        data class Error(val message: String) : BackupResult()
    }

    suspend fun buildBackupPayload(): JSONObject = withContext(Dispatchers.IO) {
        val products = repository.getAllProductsSync()
        val sales = repository.getAllSalesSync()
        val lowStock = repository.getLowStockProductsSync()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        val root = JSONObject()
        root.put("timestamp", sdf.format(Date()))
        root.put("appVersion", "1.0")

        val productsArray = JSONArray()
        products.forEach { p ->
            val obj = JSONObject()
            obj.put("barcode", p.barcode)
            obj.put("name", p.name)
            obj.put("category", p.category)
            obj.put("costPrice", p.costPrice)
            obj.put("sellingPrice", p.sellingPrice)
            obj.put("stockQuantity", p.stockQuantity)
            obj.put("unit", p.unit)
            obj.put("lowStockThreshold", p.lowStockThreshold)
            obj.put("isLowStock", p.isLowStock)
            productsArray.put(obj)
        }
        root.put("inventory", productsArray)

        val salesArray = JSONArray()
        sales.forEach { s ->
            val obj = JSONObject()
            obj.put("saleNumber", s.saleNumber)
            obj.put("date", sdf.format(Date(s.timestamp)))
            obj.put("subtotal", s.subtotal)
            obj.put("discount", s.discount)
            obj.put("tax", s.tax)
            obj.put("totalAmount", s.totalAmount)
            obj.put("paymentMethod", s.paymentMethod)
            salesArray.put(obj)
        }
        root.put("sales", salesArray)

        val lowStockArray = JSONArray()
        lowStock.forEach { ls ->
            val obj = JSONObject()
            obj.put("barcode", ls.barcode)
            obj.put("name", ls.name)
            obj.put("stockRemaining", ls.stockQuantity)
            obj.put("threshold", ls.lowStockThreshold)
            lowStockArray.put(obj)
        }
        root.put("lowStockAlerts", lowStockArray)

        root
    }

    suspend fun syncToGoogleSheets(customUrl: String? = null): BackupResult = withContext(Dispatchers.IO) {
        val targetUrl = (customUrl ?: webhookUrl).trim()
        if (targetUrl.isEmpty() || (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://"))) {
            return@withContext BackupResult.Error("Please enter a valid Google Apps Script Web App URL or Webhook endpoint.")
        }

        try {
            val payload = buildBackupPayload()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .addHeader("User-Agent", "Android-POS-Inventory-Backup/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                lastBackupTimestamp = System.currentTimeMillis()
                val productCount = payload.getJSONArray("inventory").length()
                val salesCount = payload.getJSONArray("sales").length()
                BackupResult.Success(
                    message = "Successfully synchronized $productCount products and $salesCount sales to Google Sheets!",
                    productCount = productCount,
                    salesCount = salesCount
                )
            } else {
                BackupResult.Error("Google Sheets endpoint returned HTTP ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            BackupResult.Error("Network error backing up to Google Sheets: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun pushQueuedSales(
        queuedItems: List<SyncQueueItem>,
        customUrl: String? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        val targetUrl = (customUrl ?: webhookUrl).trim()
        if (targetUrl.isEmpty() || (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://"))) {
            return@withContext BackupResult.Error("No valid Google Sheets endpoint URL configured.")
        }

        try {
            val products = repository.getAllProductsSync()
            val sales = repository.getAllSalesSync()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            val root = JSONObject()
            root.put("action", "offline_sales_push")
            root.put("timestamp", sdf.format(Date()))
            root.put("appVersion", "1.0")
            root.put("offlineQueueCount", queuedItems.size)

            val queueArray = JSONArray()
            queuedItems.forEach { q ->
                val qObj = try {
                    JSONObject(q.payloadJson)
                } catch (_: Exception) {
                    JSONObject().apply {
                        put("saleNumber", q.saleNumber)
                        put("totalAmount", q.totalAmount)
                        put("paymentMethod", q.paymentMethod)
                        put("itemCount", q.itemCount)
                    }
                }
                qObj.put("queueId", q.id)
                qObj.put("queueCreatedAt", q.createdAt)
                queueArray.put(qObj)
            }
            root.put("queuedSales", queueArray)

            // Include current full snapshot to keep Sheets in sync
            val productsArray = JSONArray()
            products.forEach { p ->
                val obj = JSONObject()
                obj.put("barcode", p.barcode)
                obj.put("name", p.name)
                obj.put("category", p.category)
                obj.put("costPrice", p.costPrice)
                obj.put("sellingPrice", p.sellingPrice)
                obj.put("stockQuantity", p.stockQuantity)
                obj.put("unit", p.unit)
                obj.put("lowStockThreshold", p.lowStockThreshold)
                obj.put("isLowStock", p.isLowStock)
                productsArray.put(obj)
            }
            root.put("inventory", productsArray)

            val salesArray = JSONArray()
            sales.forEach { s ->
                val obj = JSONObject()
                obj.put("saleNumber", s.saleNumber)
                obj.put("date", sdf.format(Date(s.timestamp)))
                obj.put("subtotal", s.subtotal)
                obj.put("discount", s.discount)
                obj.put("tax", s.tax)
                obj.put("totalAmount", s.totalAmount)
                obj.put("paymentMethod", s.paymentMethod)
                salesArray.put(obj)
            }
            root.put("sales", salesArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = root.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .addHeader("User-Agent", "Android-POS-Inventory-SyncQueue/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                lastBackupTimestamp = System.currentTimeMillis()
                BackupResult.Success(
                    message = "Successfully pushed ${queuedItems.size} offline sale(s) to Google Sheets!",
                    productCount = products.size,
                    salesCount = sales.size
                )
            } else {
                BackupResult.Error("Google Sheets endpoint returned HTTP ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            BackupResult.Error("Network error pushing queued sales: ${e.localizedMessage ?: "Connection error"}")
        }
    }

    suspend fun generateInventoryCsv(): String = withContext(Dispatchers.IO) {
        val products = repository.getAllProductsSync()
        val sb = StringBuilder()
        sb.append("Barcode,Product Name,Category,Cost Price ($),Selling Price ($),Stock Quantity,Unit,Low Stock Threshold,Stock Status\n")
        products.forEach { p ->
            val status = when {
                p.isOutOfStock -> "OUT OF STOCK"
                p.isLowStock -> "LOW STOCK"
                else -> "IN STOCK"
            }
            sb.append("\"${escapeCsv(p.barcode)}\",\"${escapeCsv(p.name)}\",\"${escapeCsv(p.category)}\",${p.costPrice},${p.sellingPrice},${p.stockQuantity},\"${escapeCsv(p.unit)}\",${p.lowStockThreshold},\"$status\"\n")
        }
        sb.toString()
    }

    suspend fun generateSalesCsv(): String = withContext(Dispatchers.IO) {
        val sales = repository.getAllSalesSync()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append("Receipt Number,Date & Time,Subtotal ($),Discount ($),Tax ($),Total Amount ($),Payment Method,Notes\n")
        sales.forEach { s ->
            sb.append("\"${escapeCsv(s.saleNumber)}\",\"${sdf.format(Date(s.timestamp))}\",${s.subtotal},${s.discount},${s.tax},${s.totalAmount},\"${escapeCsv(s.paymentMethod)}\",\"${escapeCsv(s.customerNotes)}\"\n")
        }
        sb.toString()
    }

    private fun escapeCsv(value: String): String = value.replace("\"", "\"\"")

    fun shareSpreadsheetCsv(csvContent: String, title: String): Intent {
        val cachePath = File(context.cacheDir, "exports")
        cachePath.mkdirs()
        val file = File(cachePath, "${title.replace(" ", "_")}_backup.csv")
        file.writeText(csvContent)

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "POS Backup: $title")
            putExtra(Intent.EXTRA_TEXT, "Here is your exported POS & Inventory backup file formatted for Google Sheets.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    companion object {
        const val GOOGLE_APPS_SCRIPT_TEMPLATE = """
// 1. In Google Sheets: Extensions -> Apps Script
// 2. Paste this code and click 'Deploy' -> 'New deployment'
// 3. Select type: 'Web app'
// 4. Execute as: 'Me', Who has access: 'Anyone'
// 5. Copy the Web App URL into the POS & Inventory app!

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // 1. Inventory Tab
    var invSheet = ss.getSheetByName("Inventory") || ss.insertSheet("Inventory");
    invSheet.clear();
    invSheet.appendRow(["Barcode", "Name", "Category", "Cost Price ($)", "Selling Price ($)", "Stock", "Unit", "Alert Threshold", "Status"]);
    invSheet.getRange(1, 1, 1, 9).setBackground("#0D9488").setFontColor("#FFFFFF").setFontWeight("bold");

    if (data.inventory && data.inventory.length > 0) {
      var rows = data.inventory.map(function(item) {
        var status = item.stockQuantity <= 0 ? "OUT OF STOCK" : (item.isLowStock ? "LOW STOCK" : "IN STOCK");
        return [item.barcode, item.name, item.category, item.costPrice, item.sellingPrice, item.stockQuantity, item.unit, item.lowStockThreshold, status];
      });
      invSheet.getRange(2, 1, rows.length, 9).setValues(rows);
    }

    // 2. Sales Tab
    var salesSheet = ss.getSheetByName("Sales_History") || ss.insertSheet("Sales_History");
    salesSheet.clear();
    salesSheet.appendRow(["Receipt #", "Date", "Subtotal ($)", "Discount ($)", "Tax ($)", "Total Amount ($)", "Payment Method"]);
    salesSheet.getRange(1, 1, 1, 7).setBackground("#1E293B").setFontColor("#FFFFFF").setFontWeight("bold");

    if (data.sales && data.sales.length > 0) {
      var salesRows = data.sales.map(function(s) {
        return [s.saleNumber, s.date, s.subtotal, s.discount, s.tax, s.totalAmount, s.paymentMethod];
      });
      salesSheet.getRange(2, 1, salesRows.length, 7).setValues(salesRows);
    }

    return ContentService.createTextOutput(JSON.stringify({status: "success", timestamp: new Date()}))
      .setMimeType(ContentService.MimeType.JSON);
  } catch(err) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: err.toString()}))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
"""
    }
}
