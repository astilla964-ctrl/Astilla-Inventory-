package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.PosViewModel
import com.example.ui.ScannerMode
import com.example.ui.backup.GoogleSheetsBackupScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.inventory.InventoryScreen
import com.example.ui.pos.PosRegisterScreen
import com.example.ui.scanner.CameraScannerOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PosInventoryApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosInventoryApp(viewModel: PosViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val isScannerOpen by viewModel.isScannerOpen.collectAsStateWithLifecycle()
    val scannerMode by viewModel.scannerMode.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = "POS & Inventory",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "POS & Inventory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (lowStockProducts.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Low stock",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${lowStockProducts.size} Low",
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        // Network & Sync Queue Status Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                !isOnline -> Color(0xFFFEF3C7)
                                pendingSyncCount > 0 -> Color(0xFFE0E7FF)
                                else -> Color(0xFFD1FAE5)
                            },
                            modifier = Modifier
                                .clickable { viewModel.selectTab(AppTab.BACKUP) }
                                .testTag("appbar_network_status_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        !isOnline -> Icons.Default.WifiOff
                                        pendingSyncCount > 0 -> Icons.Default.CloudSync
                                        else -> Icons.Default.Wifi
                                    },
                                    contentDescription = "Network Status",
                                    tint = when {
                                        !isOnline -> Color(0xFFD97706)
                                        pendingSyncCount > 0 -> Color(0xFF4338CA)
                                        else -> Color(0xFF059669)
                                    },
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        !isOnline -> if (pendingSyncCount > 0) "Offline (${pendingSyncCount})" else "Offline"
                                        pendingSyncCount > 0 -> "${pendingSyncCount} to sync"
                                        else -> "Online"
                                    },
                                    color = when {
                                        !isOnline -> Color(0xFF92400E)
                                        pendingSyncCount > 0 -> Color(0xFF312E81)
                                        else -> Color(0xFF065F46)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val mode = if (currentTab == AppTab.INVENTORY) ScannerMode.SCAN_TO_INVENTORY else ScannerMode.SCAN_TO_CART
                                viewModel.openScanner(mode)
                            },
                            modifier = Modifier.testTag("appbar_quick_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan with Camera",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isTablet) {
                    // Mobile Bottom Navigation Bar
                    NavigationBar(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("mobile_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.POS,
                            onClick = { viewModel.selectTab(AppTab.POS) },
                            icon = { Icon(Icons.Default.Storefront, contentDescription = "POS Register") },
                            label = { Text("POS") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_pos")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppTab.INVENTORY,
                            onClick = { viewModel.selectTab(AppTab.INVENTORY) },
                            icon = {
                                if (lowStockProducts.isNotEmpty()) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = Color(0xFFD97706)) {
                                            Text("${lowStockProducts.size}")
                                        }
                                    }) {
                                        Icon(Icons.Default.Inventory2, contentDescription = "Inventory")
                                    }
                                } else {
                                    Icon(Icons.Default.Inventory2, contentDescription = "Inventory")
                                }
                            },
                            label = { Text("Inventory") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_inventory")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppTab.DASHBOARD,
                            onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_dashboard")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppTab.BACKUP,
                            onClick = { viewModel.selectTab(AppTab.BACKUP) },
                            icon = {
                                if (pendingSyncCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = Color(0xFF0D9488)) {
                                            Text("$pendingSyncCount")
                                        }
                                    }) {
                                        Icon(Icons.Default.CloudSync, contentDescription = "Sheets Backup")
                                    }
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = "Sheets Backup")
                                }
                            },
                            label = { Text("Sheets") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_backup")
                        )
                    }
                }
            }
        ) { paddingValues ->
            if (isTablet) {
                // Tablet Layout: Navigation Rail on Left + Content on Right
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("tablet_nav_rail"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    ) {
                        NavigationRailItem(
                            selected = currentTab == AppTab.POS,
                            onClick = { viewModel.selectTab(AppTab.POS) },
                            icon = { Icon(Icons.Default.Storefront, contentDescription = "POS") },
                            label = { Text("Register") },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("rail_nav_pos")
                        )

                        NavigationRailItem(
                            selected = currentTab == AppTab.INVENTORY,
                            onClick = { viewModel.selectTab(AppTab.INVENTORY) },
                            icon = {
                                if (lowStockProducts.isNotEmpty()) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = Color(0xFFD97706)) {
                                            Text("${lowStockProducts.size}")
                                        }
                                    }) {
                                        Icon(Icons.Default.Inventory2, contentDescription = "Inventory")
                                    }
                                } else {
                                    Icon(Icons.Default.Inventory2, contentDescription = "Inventory")
                                }
                            },
                            label = { Text("Inventory") },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("rail_nav_inventory")
                        )

                        NavigationRailItem(
                            selected = currentTab == AppTab.DASHBOARD,
                            onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("rail_nav_dashboard")
                        )

                        NavigationRailItem(
                            selected = currentTab == AppTab.BACKUP,
                            onClick = { viewModel.selectTab(AppTab.BACKUP) },
                            icon = {
                                if (pendingSyncCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = Color(0xFF0D9488)) {
                                            Text("$pendingSyncCount")
                                        }
                                    }) {
                                        Icon(Icons.Default.CloudSync, contentDescription = "Sheets Backup")
                                    }
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = "Sheets Backup")
                                }
                            },
                            label = { Text("Sheets") },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("rail_nav_backup")
                        )
                    }

                    // Active screen content
                    Box(modifier = Modifier.weight(1f)) {
                        TabContent(currentTab = currentTab, viewModel = viewModel)
                    }
                }
            } else {
                // Phone Layout: Full Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    TabContent(currentTab = currentTab, viewModel = viewModel)
                }
            }
        }

        // Camera Barcode Scanner Overlay
        if (isScannerOpen) {
            CameraScannerOverlay(
                mode = scannerMode,
                onBarcodeScanned = { barcode ->
                    viewModel.handleBarcodeScanned(barcode)
                },
                onDismiss = {
                    viewModel.closeScanner()
                }
            )
        }
    }
}

@Composable
fun TabContent(
    currentTab: AppTab,
    viewModel: PosViewModel
) {
    Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
        when (tab) {
            AppTab.POS -> PosRegisterScreen(viewModel = viewModel)
            AppTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
            AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
            AppTab.BACKUP -> GoogleSheetsBackupScreen(viewModel = viewModel)
        }
    }
}
