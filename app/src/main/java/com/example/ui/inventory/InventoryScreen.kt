package com.example.ui.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.ui.PosViewModel
import com.example.ui.ScannerMode
import java.util.Locale

@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.filteredInventoryProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val isLowStockOnly by viewModel.inventoryFilterLowStockOnly.collectAsStateWithLifecycle()
    val searchQuery by viewModel.inventorySearchQuery.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.inventorySelectedCategory.collectAsStateWithLifecycle()

    val scannedInventoryProduct by viewModel.scannedProductForInventory.collectAsStateWithLifecycle()
    val scannedNewBarcode by viewModel.scannedNewBarcode.collectAsStateWithLifecycle()

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToRestock by remember { mutableStateOf<Product?>(null) }
    var isCreatingNewProduct by remember { mutableStateOf(false) }
    var newProductInitialBarcode by remember { mutableStateOf("") }

    // Handle scanned item triggers
    LaunchedEffect(scannedInventoryProduct) {
        scannedInventoryProduct?.let {
            productToRestock = it
            viewModel.clearScannedInventoryItem()
        }
    }

    LaunchedEffect(scannedNewBarcode) {
        scannedNewBarcode?.let {
            newProductInitialBarcode = it
            isCreatingNewProduct = true
            viewModel.clearScannedNewBarcode()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar + Scanner button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setInventorySearch(it) },
                    placeholder = { Text("Search inventory by name, SKU...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setInventorySearch("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inventory_search_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { viewModel.openScanner(ScannerMode.SCAN_TO_INVENTORY) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    modifier = Modifier.testTag("inventory_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Item",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Automated Low Stock Alert Banner (Real-time proactive warning)
            AnimatedVisibility(visible = lowStockProducts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { viewModel.toggleInventoryLowStockOnly() }
                        .testTag("low_stock_alert_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Low Stock Alert",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Automated Alert: ${lowStockProducts.size} item(s) low or out of stock!",
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isLowStockOnly) "Showing low stock only (tap to show all)" else "Tap to filter these items immediately",
                                    color = Color(0xFFB45309),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLowStockOnly) Color(0xFFD97706) else Color.White
                        ) {
                            Text(
                                text = if (isLowStockOnly) "Show All" else "Review",
                                color = if (isLowStockOnly) Color.White else Color(0xFF92400E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Category Filter row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && !isLowStockOnly,
                        onClick = {
                            viewModel.setInventoryCategory(null)
                            if (isLowStockOnly) viewModel.toggleInventoryLowStockOnly()
                        },
                        label = { Text("All Items (${products.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_all_items")
                    )
                }
                item {
                    FilterChip(
                        selected = isLowStockOnly,
                        onClick = { viewModel.toggleInventoryLowStockOnly() },
                        label = { Text("Low Stock Alert (${lowStockProducts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_low_stock_chip")
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory.equals(cat, ignoreCase = true),
                        onClick = {
                            if (selectedCategory.equals(cat, ignoreCase = true)) {
                                viewModel.setInventoryCategory(null)
                            } else {
                                viewModel.setInventoryCategory(cat)
                            }
                        },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inventory Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "No inventory",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the + button below or scan a barcode to add items",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("inventory_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        InventoryProductCard(
                            product = product,
                            onEdit = { productToEdit = product },
                            onRestock = { productToRestock = product },
                            onDelete = { viewModel.deleteProduct(product) }
                        )
                    }
                }
            }
        }

        // Floating Action Button: Add Product
        FloatingActionButton(
            onClick = {
                newProductInitialBarcode = ""
                isCreatingNewProduct = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_product_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Product"
            )
        }
    }

    // Add or Edit Product Dialog
    if (isCreatingNewProduct || productToEdit != null) {
        ProductFormDialog(
            product = productToEdit,
            prefilledBarcode = newProductInitialBarcode,
            onSave = { saved ->
                viewModel.saveProduct(saved)
                isCreatingNewProduct = false
                productToEdit = null
            },
            onDismiss = {
                isCreatingNewProduct = false
                productToEdit = null
            }
        )
    }

    // Quick Restock Dialog
    productToRestock?.let { product ->
        QuickRestockDialog(
            product = product,
            onRestock = { qty ->
                viewModel.quickRestock(product, qty)
                productToRestock = null
            },
            onDismiss = { productToRestock = null }
        )
    }
}

@Composable
fun InventoryProductCard(
    product: Product,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onDelete: () -> Unit
) {
    val isOut = product.isOutOfStock
    val isLow = product.isLowStock

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isOut) Color(0xFFDC2626).copy(alpha = 0.6f)
                else if (isLow) Color(0xFFD97706).copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outlineVariant
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SKU: ${product.barcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "• ${product.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Stock Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isOut -> Color(0xFFFEE2E2)
                        isLow -> Color(0xFFFEF3C7)
                        else -> Color(0xFFD1FAE5)
                    }
                ) {
                    Text(
                        text = "${product.stockQuantity} ${product.unit}",
                        color = when {
                            isOut -> Color(0xFF991B1B)
                            isLow -> Color(0xFF92400E)
                            else -> Color(0xFF065F46)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing details & Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("Retail Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text("Cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", product.costPrice)}",
                            fontSize = 13.sp
                        )
                    }
                    Column {
                        Text("Margin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", product.profitMargin)}%",
                            fontSize = 13.sp,
                            color = if (product.profitMargin > 20) Color(0xFF059669) else Color(0xFFD97706)
                        )
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onRestock,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("restock_btn_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restock",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restock", fontSize = 12.sp)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickRestockDialog(
    product: Product,
    onRestock: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf("10") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Restock Inventory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Current Stock: ${product.stockQuantity} ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Increment Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 25, 50).forEach { qty ->
                        OutlinedButton(
                            onClick = { quantityText = qty.toString() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("+$qty")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity to Add") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restock_quantity_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = quantityText.toIntOrNull() ?: 0
                            if (qty > 0) onRestock(qty)
                        },
                        modifier = Modifier.testTag("confirm_restock_button")
                    ) {
                        Text("Confirm Restock")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    product: Product?,
    prefilledBarcode: String = "",
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var barcode by remember { mutableStateOf(product?.barcode ?: prefilledBarcode) }
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Beverages") }
    var costPriceText by remember { mutableStateOf(product?.costPrice?.toString() ?: "1.50") }
    var sellingPriceText by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "3.99") }
    var stockQuantityText by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "10") }
    var lowStockThresholdText by remember { mutableStateOf(product?.lowStockThreshold?.toString() ?: "5") }
    var unit by remember { mutableStateOf(product?.unit ?: "pcs") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (product == null) "Add New Product" else "Edit Product",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode / SKU *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("product_barcode_input")
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text("Retail ($) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_price_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockQuantityText,
                        onValueChange = { stockQuantityText = it },
                        label = { Text("Current Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_stock_input")
                    )

                    OutlinedTextField(
                        value = lowStockThresholdText,
                        onValueChange = { lowStockThresholdText = it },
                        label = { Text("Alert Threshold") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && barcode.isNotBlank()) {
                                val cost = costPriceText.toDoubleOrNull() ?: 0.0
                                val price = sellingPriceText.toDoubleOrNull() ?: 0.0
                                val stock = stockQuantityText.toIntOrNull() ?: 0
                                val threshold = lowStockThresholdText.toIntOrNull() ?: 5

                                val updated = (product ?: Product(
                                    barcode = barcode.trim(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    costPrice = cost,
                                    sellingPrice = price,
                                    stockQuantity = stock,
                                    lowStockThreshold = threshold,
                                    unit = unit.trim()
                                )).copy(
                                    barcode = barcode.trim(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    costPrice = cost,
                                    sellingPrice = price,
                                    stockQuantity = stock,
                                    lowStockThreshold = threshold,
                                    unit = unit.trim(),
                                    lastUpdated = System.currentTimeMillis()
                                )

                                onSave(updated)
                            }
                        },
                        enabled = name.isNotBlank() && barcode.isNotBlank(),
                        modifier = Modifier.testTag("save_product_button")
                    ) {
                        Text("Save Product")
                    }
                }
            }
        }
    }
}
