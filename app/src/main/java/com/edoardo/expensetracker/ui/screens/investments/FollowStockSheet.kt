package com.edoardo.expensetracker.ui.screens.investments

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.entity.StockEntity
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowStockSheet(
    allStocks: List<StockEntity>,
    onDismiss: () -> Unit,
    onToggleFollow: (symbol: String, isFollowed: Boolean) -> Unit,
    onAddNewStock: (symbol: String, name: String, price: Double, assetType: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "ETF", "INDEX", "STOCK", "CRYPTO"
    var showCustomStockDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "ALL" to "Tutti",
        StockEntity.TYPE_ETF to "ETF",
        StockEntity.TYPE_INDEX to "Indici",
        StockEntity.TYPE_STOCK to "Azioni",
        StockEntity.TYPE_CRYPTO to "Crypto"
    )

    val filteredStocks = remember(searchQuery, selectedFilter, allStocks) {
        val q = searchQuery.trim().lowercase()
        allStocks.filter { stock ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                StockEntity.TYPE_ETF -> stock.assetType == StockEntity.TYPE_ETF || stock.assetType == StockEntity.TYPE_COMMODITY
                else -> stock.assetType == selectedFilter
            }
            val matchesSearch = q.isBlank() || stock.symbol.lowercase().contains(q) || stock.name.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Titolo e Tasto Aggiungi Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Segui Titoli & ETF",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Seleziona ETF, indici, azioni o crypto da monitorare",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { showCustomStockDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nuovo Ticker",
                        tint = PrimaryMint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra di Ricerca
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(PrimaryMint),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cerca (es. MSCI, VWCE, S&P 500, Apple)...",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancella",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (Tutti, ETF, Indici, Azioni, Crypto)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { (typeKey, label) ->
                    val isSelected = selectedFilter == typeKey
                    val chipColor = when (typeKey) {
                        StockEntity.TYPE_ETF -> Color(0xFF0EA5E9)
                        StockEntity.TYPE_INDEX -> Color(0xFF8B5CF6)
                        StockEntity.TYPE_CRYPTO -> Color(0xFFF59E0B)
                        else -> PrimaryMint
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedFilter = typeKey },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) chipColor.copy(alpha = 0.22f) else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) chipColor else SurfaceBorder)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) chipColor else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista Assets
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredStocks, key = { it.symbol }) { stock ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggleFollow(stock.symbol, !stock.isFollowed)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stock.symbol,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AssetTypeBadge(assetType = stock.assetType)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "€ ${String.format(Locale.ITALY, "%,.2f", stock.currentPrice)}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = stock.name,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }

                            // Switch / Toggle Segui
                            Switch(
                                checked = stock.isFollowed,
                                onCheckedChange = { isChecked ->
                                    onToggleFollow(stock.symbol, isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF0F172A),
                                    checkedTrackColor = PrimaryMint,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceBorder
                                )
                            )
                        }
                    }
                }

                if (filteredStocks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nessun titolo trovato per \"$searchQuery\"",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showCustomStockDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint)
                            ) {
                                Text("Aggiungi Ticker Personalizzato", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomStockDialog) {
        CustomStockDialog(
            initialSymbol = searchQuery.trim().uppercase(),
            onDismiss = { showCustomStockDialog = false },
            onConfirm = { sym, name, price, assetType ->
                onAddNewStock(sym, name, price, assetType)
                showCustomStockDialog = false
            }
        )
    }
}

@Composable
fun AssetTypeBadge(assetType: String) {
    val (label, bg, fg) = when (assetType) {
        StockEntity.TYPE_ETF -> Triple("ETF", Color(0xFF0EA5E9).copy(alpha = 0.18f), Color(0xFF38BDF8))
        StockEntity.TYPE_INDEX -> Triple("INDICE", Color(0xFF8B5CF6).copy(alpha = 0.18f), Color(0xFFA78BFA))
        StockEntity.TYPE_CRYPTO -> Triple("CRYPTO", Color(0xFFF59E0B).copy(alpha = 0.18f), Color(0xFFFBBF24))
        StockEntity.TYPE_COMMODITY -> Triple("COMMODITY", Color(0xFFEC4899).copy(alpha = 0.18f), Color(0xFFF472B6))
        else -> Triple("AZIONE", PrimaryMint.copy(alpha = 0.15f), PrimaryMint)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun CustomStockDialog(
    initialSymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, name: String, price: Double, assetType: String) -> Unit
) {
    var symbol by remember { mutableStateOf(initialSymbol) }
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(StockEntity.TYPE_ETF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text("Aggiungi Asset Personalizzato", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Tipo di asset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        StockEntity.TYPE_ETF to "ETF",
                        StockEntity.TYPE_INDEX to "Indice",
                        StockEntity.TYPE_STOCK to "Azione",
                        StockEntity.TYPE_CRYPTO to "Crypto"
                    ).forEach { (typeKey, label) ->
                        val isSelected = selectedType == typeKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedType = typeKey },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PrimaryMint.copy(alpha = 0.22f) else SurfaceBorder,
                            border = BorderStroke(1.dp, if (isSelected) PrimaryMint else Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryMint else TextSecondary
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Simbolo Ticker (es. VWCE, SWDA, ENI)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Asset o Fondo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Prezzo Iniziale (€)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = priceStr.toDoubleOrNull() ?: 10.0
                    if (symbol.isNotBlank()) {
                        onConfirm(symbol, name, p, selectedType)
                    }
                }
            ) {
                Text("Aggiungi", color = PrimaryMint, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondary)
            }
        }
    )
}
