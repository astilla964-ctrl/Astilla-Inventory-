package com.example.ui.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.data.repository.PosRepository
import com.example.ui.PosViewModel
import com.example.ui.ScannerMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosRegisterScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.filteredPosProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.posSelectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.posSearchQuery.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val lastReceipt by viewModel.lastCompletedReceipt.collectAsStateWithLifecycle()

    var showMobileCartSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // Tablet Side-by-Side View
            Row(modifier = Modifier.fillMaxSize()) {
                // Left 60%: Catalog & Search
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    PosHeaderSearchBar(
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setPosSearch(it) },
                        onScanClick = { viewModel.openScanner(ScannerMode.SCAN_TO_CART) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CategoryFilterRow(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelect = { viewModel.setPosCategory(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProductCatalogGrid(
                        products = products,
                        columns = 3,
                        onProductClick = { viewModel.addToCart(it, 1) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Right 40%: Active Cart & Checkout Terminal
                CartRegisterPane(
                    viewModel = viewModel,
                    cartItems = cartItems,
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                )
            }
        } else {
            // Phone Single Column View with Floating Bottom Cart Bar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                PosHeaderSearchBar(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setPosSearch(it) },
                    onScanClick = { viewModel.openScanner(ScannerMode.SCAN_TO_CART) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CategoryFilterRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.setPosCategory(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProductCatalogGrid(
                    products = products,
                    columns = 2,
                    onProductClick = { viewModel.addToCart(it, 1) },
                    modifier = Modifier.weight(1f)
                )

                // Mobile Bottom Cart Summary Pill
                AnimatedVisibility(visible = cartItems.isNotEmpty()) {
                    val rawTotal = cartItems.sumOf { it.subtotal }
                    val totalQty = cartItems.sumOf { it.quantity }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { showMobileCartSheet = true }
                            .testTag("mobile_cart_pill")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$totalQty item${if (totalQty > 1) "s" else ""}",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", rawTotal)}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { showMobileCartSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("checkout_button_mobile")
                            ) {
                                Text("Checkout", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Mobile Checkout Bottom Sheet
            if (showMobileCartSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMobileCartSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    CartRegisterPane(
                        viewModel = viewModel,
                        cartItems = cartItems,
                        onCheckoutFinished = { showMobileCartSheet = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    // Receipt Confirmation Dialog
    lastReceipt?.let { receipt ->
        ReceiptDialog(
            receipt = receipt,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

@Composable
fun PosHeaderSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScanClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by name, barcode, category...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Search"
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("pos_search_input")
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onScanClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            modifier = Modifier.testTag("pos_barcode_scan_button")
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan Barcode",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Scan", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Items") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("category_chip_all")
            )
        }
        items(categories) { cat ->
            FilterChip(
                selected = selectedCategory.equals(cat, ignoreCase = true),
                onClick = {
                    if (selectedCategory.equals(cat, ignoreCase = true)) {
                        onCategorySelect(null)
                    } else {
                        onCategorySelect(cat)
                    }
                },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("category_chip_$cat")
            )
        }
    }
}

@Composable
fun ProductCatalogGrid(
    products: List<Product>,
    columns: Int,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "No products",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No matching products found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.testTag("pos_catalog_grid")
        ) {
            items(products, key = { it.id }) { product ->
                ProductCatalogCard(
                    product = product,
                    onClick = { onProductClick(product) }
                )
            }
        }
    }
}

@Composable
fun ProductCatalogCard(
    product: Product,
    onClick: () -> Unit
) {
    val isOut = product.isOutOfStock
    val isLow = product.isLowStock

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isOut) Color(0xFFDC2626).copy(alpha = 0.5f)
                else if (isLow) Color(0xFFD97706).copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outlineVariant
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOut) 0.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOut, onClick = onClick)
            .testTag("pos_item_${product.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Stock status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isOut -> Color(0xFFFEE2E2)
                        isLow -> Color(0xFFFEF3C7)
                        else -> Color(0xFFD1FAE5)
                    }
                ) {
                    Text(
                        text = when {
                            isOut -> "OUT"
                            isLow -> "LOW (${product.stockQuantity})"
                            else -> "${product.stockQuantity} ${product.unit}"
                        },
                        color = when {
                            isOut -> Color(0xFF991B1B)
                            isLow -> Color(0xFF92400E)
                            else -> Color(0xFF065F46)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.barcode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isOut) Color.Gray.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = if (isOut) Color.Gray else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartRegisterPane(
    viewModel: PosViewModel,
    cartItems: List<PosRepository.CartItem>,
    modifier: Modifier = Modifier,
    onCheckoutFinished: (() -> Unit)? = null
) {
    val discountPercent by viewModel.discountPercent.collectAsStateWithLifecycle()
    val taxPercent by viewModel.taxPercent.collectAsStateWithLifecycle()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val cashTendered by viewModel.cashTendered.collectAsStateWithLifecycle()

    val rawSubtotal = cartItems.sumOf { it.subtotal }
    val discount = rawSubtotal * (discountPercent / 100.0)
    val discountedSubtotal = rawSubtotal - discount
    val tax = discountedSubtotal * (taxPercent / 100.0)
    val grandTotal = discountedSubtotal + tax

    val cashGivenDouble = cashTendered.toDoubleOrNull() ?: 0.0
    val changeDue = (cashGivenDouble - grandTotal).coerceAtLeast(0.0)

    Column(modifier = modifier) {
        // Cart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (cartItems.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearCart() },
                    modifier = Modifier.testTag("clear_cart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Empty cart",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Cart is empty",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Scan or tap items to build ticket",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // Cart Items Scrollable List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("cart_items_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { viewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                        onDecrease = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                        onRemove = { viewModel.removeFromCart(item.product.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calculations & Tender Summary
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text("$${String.format(Locale.US, "%.2f", rawSubtotal)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Discount Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Discount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.0, 5.0, 10.0, 15.0).forEach { disc ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (discountPercent == disc) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable { viewModel.setDiscount(disc) }
                            ) {
                                Text(
                                    text = "${disc.toInt()}%",
                                    color = if (discountPercent == disc) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tax ($taxPercent%)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text("$${String.format(Locale.US, "%.2f", tax)}", fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", grandTotal)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Payment Method Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("CASH" to Icons.Default.LocalAtm, "CARD" to Icons.Default.CreditCard, "DIGITAL" to Icons.Default.Payments).forEach { (method, icon) ->
                val isSelected = selectedPaymentMethod == method
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setPaymentMethod(method) }
                        .testTag("payment_method_$method")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = method,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Cash tender quick buttons if CASH selected
        if (selectedPaymentMethod == "CASH") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(grandTotal, 20.0, 50.0, 100.0).distinct().forEach { quickCash ->
                    OutlinedButton(
                        onClick = { viewModel.setCashTendered(String.format(Locale.US, "%.2f", quickCash)) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("$$quickCash", fontSize = 11.sp)
                    }
                }
            }

            if (cashGivenDouble > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Change Due:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", changeDue)}",
                        fontWeight = FontWeight.Bold,
                        color = if (cashGivenDouble >= grandTotal) Color(0xFF059669) else Color(0xFFDC2626),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Complete Checkout Button
        Button(
            onClick = {
                viewModel.completeCheckout()
                onCheckoutFinished?.invoke()
            },
            enabled = cartItems.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("complete_checkout_button")
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Charge $${String.format(Locale.US, "%.2f", grandTotal)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: PosRepository.CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_row_${item.product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", item.product.sellingPrice)} × ${item.quantity} = $${String.format(Locale.US, "%.2f", item.subtotal)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    receipt: PosRepository.CheckoutResult,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFD1FAE5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Payment Received!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = receipt.saleNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Charged", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", receipt.totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp
                            )
                        }

                        if (receipt.lowStockAlertsTriggered.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Low stock alert",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alert: ${receipt.lowStockAlertsTriggered.size} item(s) now at or below low-stock limit!",
                                    color = Color(0xFFD97706),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_receipt_button")
                ) {
                    Text("New Sale", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
