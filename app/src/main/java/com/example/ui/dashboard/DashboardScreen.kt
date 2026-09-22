package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Product
import com.example.ui.PosViewModel
import com.example.ui.TimeframeFilter
import com.example.ui.TrendPoint
import com.example.ui.inventory.QuickRestockDialog
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val timeframe by viewModel.timeframeFilter.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()

    var productToRestock by remember { mutableStateOf<Product?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 12.dp)
                .testTag("dashboard_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Header with Realtime live tag
            item {
                DashboardHeroBanner()
            }

            // Timeframe Filter selector
            item {
                TimeframeSelectorRow(
                    currentTimeframe = timeframe,
                    onSelectTimeframe = { viewModel.setTimeframe(it) }
                )
            }

            // Real-Time KPI Cards Grid
            item {
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiMetricCard(
                            title = "Total Revenue",
                            value = "$${String.format(Locale.US, "%.2f", metrics.totalRevenue)}",
                            subtext = "Gross Sales in period",
                            icon = Icons.Default.AttachMoney,
                            accentColor = Color(0xFF0D9488),
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Est. Gross Profit",
                            value = "$${String.format(Locale.US, "%.2f", metrics.totalProfit)}",
                            subtext = "~45% gross margin",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Transactions",
                            value = "${metrics.transactionCount}",
                            subtext = "Avg ticket $${String.format(Locale.US, "%.2f", metrics.avgTicket)}",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            accentColor = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Low Stock Alerts",
                            value = "${lowStockProducts.size}",
                            subtext = "Needs restock",
                            icon = Icons.Default.Warning,
                            accentColor = if (lowStockProducts.isNotEmpty()) Color(0xFFD97706) else Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Total Revenue",
                                value = "$${String.format(Locale.US, "%.2f", metrics.totalRevenue)}",
                                subtext = "Gross Sales",
                                icon = Icons.Default.AttachMoney,
                                accentColor = Color(0xFF0D9488),
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Est. Profit",
                                value = "$${String.format(Locale.US, "%.2f", metrics.totalProfit)}",
                                subtext = "Margin ~45%",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                accentColor = Color(0xFF059669),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Orders / Sales",
                                value = "${metrics.transactionCount}",
                                subtext = "Avg: $${String.format(Locale.US, "%.2f", metrics.avgTicket)}",
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                accentColor = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Low Stock Alert",
                                value = "${lowStockProducts.size}",
                                subtext = if (lowStockProducts.isEmpty()) "Healthy" else "Action needed",
                                icon = Icons.Default.Warning,
                                accentColor = if (lowStockProducts.isNotEmpty()) Color(0xFFD97706) else Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Historical Revenue Trend Chart
            item {
                HistoricalTrendsCard(trendPoints = metrics.trendPoints)
            }

            // Top Selling Products Leaderboard
            item {
                TopSellingProductsCard(topProducts = metrics.topProducts)
            }

            // Automated Low Stock Alerts Center
            if (lowStockProducts.isNotEmpty()) {
                item {
                    LowStockAlertsCenterCard(
                        lowStockProducts = lowStockProducts,
                        onRestockClick = { productToRestock = it }
                    )
                }
            }

            // Category Distribution Breakdown
            item {
                CategoryBreakdownCard(breakdown = metrics.categoryBreakdown)
            }
        }
    }

    // Quick restock dialog from dashboard
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
fun DashboardHeroBanner() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_pos_banner),
                contentDescription = "POS Dashboard Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay for typography readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF042F2E).copy(alpha = 0.92f),
                                Color(0xFF0F766E).copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REAL-TIME REPORTING ACTIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Column {
                    Text(
                        text = "Store Analytics & Trends",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Live inventory tracking with automated stock alerts",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TimeframeSelectorRow(
    currentTimeframe: TimeframeFilter,
    onSelectTimeframe: (TimeframeFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf(
            TimeframeFilter.TODAY to "Today",
            TimeframeFilter.WEEK to "Past 7 Days",
            TimeframeFilter.MONTH to "Past 30 Days",
            TimeframeFilter.ALL to "All Time"
        ).forEach { (filter, label) ->
            item {
                FilterChip(
                    selected = currentTimeframe == filter,
                    onClick = { onSelectTimeframe(filter) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("timeframe_chip_${filter.name}")
                )
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun HistoricalTrendsCard(trendPoints: List<TrendPoint>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("historical_trends_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historical Sales Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Revenue ($)",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Line Chart with Gradient Fill
            if (trendPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales in selected timeframe yet",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )
                }
            } else {
                val maxRevenue = (trendPoints.maxOfOrNull { it.revenue } ?: 100.0).coerceAtLeast(10.0)
                val primaryTeal = MaterialTheme.colorScheme.primary

                Column {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val stepX = if (trendPoints.size > 1) width / (trendPoints.size - 1) else width / 2

                        val path = Path()
                        val fillPath = Path()

                        val points = trendPoints.mapIndexed { index, point ->
                            val x = if (trendPoints.size > 1) index * stepX else width / 2
                            val y = height - (point.revenue.toFloat() / maxRevenue.toFloat() * (height * 0.8f)) - 10f
                            Offset(x, y)
                        }

                        // Draw background horizontal grid lines
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val gridY = height * (i.toFloat() / gridLines)
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.15f),
                                start = Offset(0f, gridY),
                                end = Offset(width, gridY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        if (points.isNotEmpty()) {
                            path.moveTo(points.first().x, points.first().y)
                            fillPath.moveTo(points.first().x, height)
                            fillPath.lineTo(points.first().x, points.first().y)

                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val controlX = (prev.x + curr.x) / 2
                                path.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                                fillPath.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                            }

                            fillPath.lineTo(points.last().x, height)
                            fillPath.close()

                            // Draw subtle gradient fill beneath line
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryTeal.copy(alpha = 0.35f),
                                        primaryTeal.copy(alpha = 0.02f)
                                    )
                                )
                            )

                            // Draw main curve stroke
                            drawPath(
                                path = path,
                                color = primaryTeal,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw data point circles
                            points.forEach { pt ->
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = primaryTeal,
                                    radius = 3.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }

                    // X-Axis Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        trendPoints.forEach { point ->
                            Text(
                                text = point.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopSellingProductsCard(topProducts: List<com.example.data.db.TopProductStat>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Top Selling Products",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topProducts.isEmpty()) {
                Text(
                    text = "No sales recorded yet to calculate top items.",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            } else {
                val maxUnits = topProducts.maxOfOrNull { it.totalQuantitySold } ?: 1
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topProducts.forEachIndexed { index, item ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = item.productName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.totalRevenue)} (${item.totalQuantitySold} sold)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (item.totalQuantitySold.toFloat() / maxUnits.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LowStockAlertsCenterCard(
    lowStockProducts: List<Product>,
    onRestockClick: (Product) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_low_stock_center")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automated Low-Stock Alert Hub",
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD97706)
                ) {
                    Text(
                        text = "${lowStockProducts.size} Items Critical",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lowStockProducts.take(4).forEach { product ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Only ${product.stockQuantity} ${product.unit} left (Threshold: ${product.lowStockThreshold})",
                                    fontSize = 11.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = { onRestockClick(product) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD97706),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Restock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(breakdown: Map<String, Double>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Inventory Value by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totalInventoryValue = breakdown.values.sum().coerceAtLeast(1.0)
            val colors = listOf(
                Color(0xFF0D9488),
                Color(0xFF3B82F6),
                Color(0xFFF59E0B),
                Color(0xFF8B5CF6),
                Color(0xFFEC4899),
                Color(0xFF10B981)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdown.entries.toList().forEachIndexed { index, (category, value) ->
                    val color = colors[index % colors.size]
                    val percentage = (value / totalInventoryValue) * 100.0
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(category, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", value)} (${String.format(Locale.US, "%.1f", percentage)}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (percentage / 100f).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
