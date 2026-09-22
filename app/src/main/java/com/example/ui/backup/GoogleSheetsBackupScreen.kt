package com.example.ui.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.GoogleSheetsBackupService
import com.example.data.model.SyncQueueItem
import com.example.data.model.SyncStatus
import com.example.ui.PosViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoogleSheetsBackupScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val webhookUrl by viewModel.webhookUrlInput.collectAsStateWithLifecycle()
    val autoBackup by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val allQueue by viewModel.allSyncQueue.collectAsStateWithLifecycle()
    val pendingQueue by viewModel.pendingSyncQueue.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()

    var showScriptCodeDialog by remember { mutableStateOf(false) }
    var selectedPreviewTab by remember { mutableStateOf("INVENTORY") }
    var copiedScriptToast by remember { mutableStateOf(false) }

    val lastBackupTime = viewModel.backupService.lastBackupTimestamp
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("backup_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card: Local Room DB + Google Sheets Backup
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF0D9488).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Google Sheets Backup Hub",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Primary Data: Local Room DB (Offline)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (lastBackupTime > 0) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (lastBackupTime > 0) "SYNCED" else "NOT SYNCED",
                                color = if (lastBackupTime > 0) Color(0xFF065F46) else Color(0xFF92400E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Google Sheets Backup:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (lastBackupTime > 0) sdf.format(Date(lastBackupTime)) else "Never",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Local Records Ready:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${allProducts.size} products • ${allSales.size} transactions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Backup on Checkout Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto-sync to Sheets on each Sale",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Silently sends new sales & updated inventory",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = autoBackup,
                            onCheckedChange = { viewModel.toggleAutoBackup() },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("auto_backup_switch")
                        )
                    }
                }
            }
        }

        // Local Room Synchronization Queue Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_queue_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isOnline) Color(0xFF10B981).copy(alpha = 0.15f)
                                        else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = if (isOnline) Color(0xFF059669) else Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Offline Sync Queue (Room DB)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isOnline) "Connection Active • Auto-Sync Ready" else "Device Offline • Queuing Sales Locally",
                                    fontSize = 11.sp,
                                    color = if (isOnline) Color(0xFF059669) else Color(0xFFD97706),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Pending Count Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (pendingCount > 0) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = if (pendingCount > 0) "$pendingCount PENDING" else "0 PENDING",
                                color = if (pendingCount > 0) Color(0xFFB45309) else Color(0xFF065F46),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sales recorded while offline are queued in SQLite via Room. Once internet connection is restored, pending sales are automatically pushed to Google Sheets.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Queue Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.processSyncQueue() },
                            enabled = !isBackingUp && pendingCount > 0,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("push_pending_queue_button")
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (pendingCount > 0) "Push Pending ($pendingCount)" else "Queue Clear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val hasFailedItems = allQueue.any { it.isFailed }
                        if (hasFailedItems) {
                            OutlinedButton(
                                onClick = { viewModel.retryFailedQueue() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("retry_failed_queue_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry Failed", fontSize = 12.sp)
                            }
                        }

                        val hasSyncedItems = allQueue.any { it.isSynced }
                        if (hasSyncedItems) {
                            OutlinedButton(
                                onClick = { viewModel.clearCompletedQueue() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("clear_synced_queue_button")
                            ) {
                                Text("Clear Synced", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Queue Items List
                    if (allQueue.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "All sales synchronized! Sales made offline will appear here and auto-push on reconnect.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Queue Items (${allQueue.size} in Room):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )

                            allQueue.take(8).forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sync_queue_item_${item.id}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = item.saleNumber,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${sdf.format(Date(item.createdAt))} • ${item.itemCount} items • ${item.paymentMethod}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "$${String.format(Locale.US, "%.2f", item.totalAmount)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                // Status badge
                                                when (item.syncStatus) {
                                                    SyncStatus.PENDING -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFFEF3C7)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.HourglassEmpty,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFFB45309),
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(3.dp))
                                                                Text("Pending", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                            }
                                                        }
                                                    }
                                                    SyncStatus.SYNCING -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFE0E7FF)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(10.dp),
                                                                    strokeWidth = 1.5.dp,
                                                                    color = Color(0xFF4338CA)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Pushing...", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4338CA))
                                                            }
                                                        }
                                                    }
                                                    SyncStatus.SYNCED -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFD1FAE5)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF065F46),
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(3.dp))
                                                                Text("Synced", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                                            }
                                                        }
                                                    }
                                                    SyncStatus.FAILED -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFFEE2E2)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Error,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF991B1B),
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(3.dp))
                                                                Text("Failed (${item.retryCount})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                                                            }
                                                        }
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteQueueItem(item.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove queue item",
                                                        tint = MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (!item.lastError.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Error: ${item.lastError}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Google Apps Script Webhook Configuration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Google Sheets Web App Endpoint",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter the Web App URL generated from your Google Sheet's Apps Script.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { viewModel.setWebhookUrl(it) },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "URL",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sheets_webhook_url_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerGoogleSheetsSync() },
                            enabled = !isBackingUp,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("sync_google_sheets_button")
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync to Google Sheets", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Google Apps Script", GoogleSheetsBackupService.GOOGLE_APPS_SCRIPT_TEMPLATE)
                                clipboard.setPrimaryClip(clip)
                                copiedScriptToast = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("copy_script_button")
                        ) {
                            Icon(
                                imageVector = if (copiedScriptToast) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy script",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (copiedScriptToast) "Copied!" else "Copy Script")
                        }
                    }

                    backupStatusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (msg.contains("Success", ignoreCase = true)) Color(0xFF059669) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Direct CSV Export & Share Option
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Export & Open Directly in Google Sheets (CSV)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Instantly generates standardized Google Sheets CSV files and launches the Android sharing menu to open in Google Sheets or Drive.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val csv = viewModel.backupService.generateInventoryCsv()
                                    val intent = viewModel.backupService.shareSpreadsheetCsv(csv, "Inventory")
                                    context.startActivity(android.content.Intent.createChooser(intent, "Open in Google Sheets"))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_inventory_csv_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Inventory CSV", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val csv = viewModel.backupService.generateSalesCsv()
                                    val intent = viewModel.backupService.shareSpreadsheetCsv(csv, "Sales_History")
                                    context.startActivity(android.content.Intent.createChooser(intent, "Open in Google Sheets"))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_sales_csv_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Sales CSV", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Live Preview of Local Backup Data
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Spreadsheet Payload Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedPreviewTab == "INVENTORY",
                                onClick = { selectedPreviewTab = "INVENTORY" },
                                label = { Text("Inventory (${allProducts.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = selectedPreviewTab == "SALES",
                                onClick = { selectedPreviewTab = "SALES" },
                                label = { Text("Sales (${allSales.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedPreviewTab == "INVENTORY") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            allProducts.take(6).forEach { product ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Text("SKU: ${product.barcode} • ${product.category}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text(
                                            text = "${product.stockQuantity} ${product.unit} @ $${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            allSales.take(6).forEach { sale ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(sale.saleNumber, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Text(sdf.format(Date(sale.timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", sale.totalAmount)} (${sale.paymentMethod})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
