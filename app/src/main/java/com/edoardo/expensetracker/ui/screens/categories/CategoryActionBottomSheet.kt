package com.edoardo.expensetracker.ui.screens.categories

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.CategoryWithSubcategories
import com.edoardo.expensetracker.data.local.dao.TransactionDetail
import com.edoardo.expensetracker.data.local.entity.SubcategoryEntity
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.components.IconAndColorPickerDialog
import com.edoardo.expensetracker.ui.components.TransactionRowItem
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

enum class CategorySheetMode {
    ACTIONS,
    EDIT_CATEGORY,
    EDIT_SUBCATEGORY
}

/**
 * Bottom Sheet Azioni Categoria (Aperta con Long-Press sulla categoria in Overview).
 * Layout integrato a dimensione fissa con transizioni interne animate:
 * - ACTIONS: Card categoria, barre spesa/percentuale, lista sottocategorie cliccabili, 3 bottoni rapidi
 * - TRANSACTIONS: Storico transazioni della categoria
 * - EDIT_CATEGORY: Modifica categoria in-place (nome, icona/colore, archivio, elimina)
 * - EDIT_SUBCATEGORY: Modifica sottocategoria in-place (nome, icona, archivio, elimina)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryActionBottomSheet(
    categoryWithSubs: CategoryWithSubcategories,
    categorySpent: Double,
    periodTotal: Double,
    periodLabel: String,
    transactions: List<TransactionDetail>,
    onDismiss: () -> Unit,
    onSaveCategory: (name: String, colorHex: String, iconName: String, isArchived: Boolean) -> Unit = { _, _, _, _ -> },
    onAddSubcategory: (name: String, iconName: String) -> Unit = { _, _ -> },
    onToggleSubcategoryArchived: (SubcategoryEntity) -> Unit = {},
    onDeleteSubcategory: (SubcategoryEntity) -> Unit = {},
    onUpdateSubcategory: (SubcategoryEntity) -> Unit = {},
    onDeleteCategory: (() -> Unit)? = null,
    onBudgetClick: () -> Unit,
    onTransactionClick: (Long) -> Unit = {}
) {
    val category = categoryWithSubs.category
    val isIncome = category.type == TransactionType.INCOME

    var sheetMode by remember { mutableStateOf(CategorySheetMode.ACTIONS) }
    var showInlineTransactions by remember { mutableStateOf(false) }

    // State per Edit Categoria
    var catName by remember(category.id, category.name) { mutableStateOf(category.name) }
    var catColorHex by remember(category.id, category.colorHex) { mutableStateOf(category.colorHex) }
    var catIcon by remember(category.id, category.iconName) { mutableStateOf(category.iconName) }
    var catArchived by remember(category.id, category.isArchived) { mutableStateOf(category.isArchived) }
    var showCatIconColorPicker by remember { mutableStateOf(false) }

    // State per Edit Sottocategoria
    var selectedSubForEdit by remember { mutableStateOf<SubcategoryEntity?>(null) }
    var editSubName by remember { mutableStateOf("") }
    var editSubIcon by remember { mutableStateOf("label") }
    var editSubArchived by remember { mutableStateOf(false) }
    var showSubIconPicker by remember { mutableStateOf(false) }

    // Dialog aggiunta sottocategoria
    var showAddSubDialog by remember { mutableStateOf(false) }
    var newSubName by remember { mutableStateOf("") }
    var newSubIcon by remember { mutableStateOf("label") }
    var showNewSubIconPicker by remember { mutableStateOf(false) }

    // Dialog conferma eliminazione categoria
    var showDeleteCatConfirm by remember { mutableStateOf(false) }

    val categoryColor = try {
        Color(android.graphics.Color.parseColor(catColorHex))
    } catch (_: Exception) {
        PrimaryMint
    }

    val cardCorner = RoundedCornerShape(24.dp)

    val categoryPercent = if (periodTotal > 0) {
        ((categorySpent / periodTotal) * 100).coerceIn(0.0, 100.0)
    } else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize(tween(260))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            AnimatedContent(
                targetState = sheetMode,
                transitionSpec = {
                    if (targetState == CategorySheetMode.ACTIONS) {
                        (slideInHorizontally(tween(260)) { -it / 2 } + fadeIn(tween(240)))
                            .togetherWith(slideOutHorizontally(tween(240)) { it } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(260)) { it } + fadeIn(tween(240)))
                            .togetherWith(slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(200)))
                    }
                },
                label = "category_sheet_mode_transition"
            ) { mode ->
                when (mode) {
                    CategorySheetMode.ACTIONS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            // Card Elegante Categoria (stile FinTechHeaderCard coerente)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardCorner),
                                shape = cardCorner,
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, SurfaceBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    SurfaceElevated,
                                                    SurfaceDark.copy(alpha = 0.95f)
                                                )
                                            )
                                        )
                                        .padding(18.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Riga 1: Nome Categoria e conteggio
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = category.name,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${transactions.size} transazioni",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextSecondary
                                                )
                                            }

                                            // Icona tonda grande (46dp) con colore categoria
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(categoryColor.copy(alpha = 0.18f))
                                                    .border(1.dp, categoryColor.copy(alpha = 0.50f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIconByName(category.iconName),
                                                    contentDescription = category.name,
                                                    tint = categoryColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Riga 2: Importo Categoria e Percentuale sul totale
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "€ ${String.format(Locale.ITALY, "%,.2f", categorySpent)}",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextPrimary
                                            )

                                            Text(
                                                text = String.format(Locale.ITALY, "%.1f%% del totale", categoryPercent),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = categoryColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Barra di Progresso
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth((categoryPercent / 100f).toFloat().coerceIn(0f, 1f))
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(categoryColor)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Etichetta periodo e spesa
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isIncome) "Entrate ($periodLabel)" else "Uscite ($periodLabel)",
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                            Text(
                                                text = "Totale: € ${String.format(Locale.ITALY, "%,.2f", periodTotal)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sezione Sottocategorie con barre di riempimento (SOLO LETTURA nel menu azioni)
                            val activeSubs = categoryWithSubs.subcategories.filter { !it.isArchived }
                            if (activeSubs.isNotEmpty()) {
                                Text(
                                    text = "Sottocategorie",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceElevated)
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    activeSubs.forEach { sub ->
                                        val subSpent = transactions
                                            .filter { it.subcategoryId == sub.id }
                                            .sumOf { it.amount }

                                        val subPct = if (categorySpent > 0) {
                                            ((subSpent / categorySpent) * 100).coerceIn(0.0, 100.0)
                                        } else 0.0

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(categoryColor.copy(alpha = 0.16f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = getCategoryIconByName(sub.iconName),
                                                            contentDescription = sub.name,
                                                            tint = categoryColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = sub.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextPrimary
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "€ ${String.format(Locale.ITALY, "%,.2f", subSpent)}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = String.format(Locale.ITALY, "(%.0f%%)", subPct),
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(3.dp))

                                            // Mini barra di riempimento sottocategoria
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth((subPct / 100f).toFloat().coerceIn(0f, 1f))
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(categoryColor.copy(alpha = 0.7f))
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                            } else {
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // I 3 Tasti di Azione Rapida (posizionati direttamente dopo le sottocategorie)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                RoundCategoryActionButton(
                                    icon = Icons.Default.Edit,
                                    label = "Modifica",
                                    iconColor = Color(0xFFF59E0B),
                                    onClick = {
                                        catName = category.name
                                        catColorHex = category.colorHex
                                        catIcon = category.iconName
                                        catArchived = category.isArchived
                                        sheetMode = CategorySheetMode.EDIT_CATEGORY
                                    }
                                )

                                if (!isIncome) {
                                    RoundCategoryActionButton(
                                        icon = Icons.Default.Savings,
                                        label = "Budget",
                                        iconColor = PrimaryMint,
                                        onClick = onBudgetClick
                                    )
                                }

                                RoundCategoryActionButton(
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                    label = "Transazioni",
                                    iconColor = AccentSky,
                                    onClick = {
                                        showInlineTransactions = !showInlineTransactions
                                    }
                                )
                            }

                            // Transazioni inline visibili direttamente sotto i 3 pulsanti
                            AnimatedVisibility(
                                visible = showInlineTransactions,
                                enter = expandVertically(tween(260)) + fadeIn(tween(240)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(180))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cronologia Transazioni",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${transactions.size} transazioni",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (transactions.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Nessuna transazione per questa categoria",
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(transactions) { tx ->
                                                TransactionRowItem(
                                                    transaction = tx,
                                                    onClick = {
                                                        onDismiss()
                                                        onTransactionClick(tx.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CategorySheetMode.EDIT_CATEGORY -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(bottom = 12.dp)
                        ) {
                            // Top Bar: Indietro, Titolo, e Salva
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { sheetMode = CategorySheetMode.ACTIONS },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Indietro",
                                            tint = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Modifica Categoria",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (catName.isNotBlank()) {
                                            onSaveCategory(catName.trim(), catColorHex, catIcon, catArchived)
                                            sheetMode = CategorySheetMode.ACTIONS
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = categoryColor, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text("Salva", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 520.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Header Nome + Squircle Icona/Colore
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        color = SurfaceElevated,
                                        border = BorderStroke(1.dp, SurfaceBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Nome",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                BasicTextField(
                                                    value = catName,
                                                    onValueChange = { catName = it },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        color = TextPrimary,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    cursorBrush = SolidColor(categoryColor),
                                                    decorationBox = { inner ->
                                                        if (catName.isEmpty()) {
                                                            Text("Nome categoria...", fontSize = 18.sp, color = TextMuted)
                                                        }
                                                        inner()
                                                    }
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(categoryColor)
                                                    .clickable { showCatIconColorPicker = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIconByName(catIcon),
                                                    contentDescription = "Cambia icona e colore",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Sezione Sottocategorie con tasto + Aggiungi
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Sottocategorie",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = categoryColor
                                        )
                                        TextButton(onClick = {
                                            newSubName = ""
                                            newSubIcon = "label"
                                            showAddSubDialog = true
                                        }) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = categoryColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Aggiungi", color = categoryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = SurfaceElevated,
                                        border = BorderStroke(1.dp, SurfaceBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (categoryWithSubs.subcategories.isEmpty()) {
                                                Text(
                                                    "Nessuna sottocategoria creata",
                                                    fontSize = 12.sp,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(vertical = 10.dp)
                                                )
                                            } else {
                                                categoryWithSubs.subcategories.forEach { sub ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                selectedSubForEdit = sub
                                                                editSubName = sub.name
                                                                editSubIcon = sub.iconName
                                                                editSubArchived = sub.isArchived
                                                                sheetMode = CategorySheetMode.EDIT_SUBCATEGORY
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(24.dp)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(categoryColor.copy(alpha = 0.20f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = getCategoryIconByName(sub.iconName),
                                                                    contentDescription = null,
                                                                    tint = categoryColor,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = sub.name,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = if (sub.isArchived) TextMuted else TextPrimary
                                                            )
                                                        }
                                                        Icon(
                                                            Icons.Default.ChevronRight,
                                                            contentDescription = null,
                                                            tint = TextMuted,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Switch Archiviazione Categoria
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = SurfaceElevated,
                                        border = BorderStroke(1.dp, SurfaceBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Archivia Categoria", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                Text("Nascondi dalla dashboard", fontSize = 11.sp, color = TextMuted)
                                            }
                                            Switch(
                                                checked = catArchived,
                                                onCheckedChange = { catArchived = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = categoryColor)
                                            )
                                        }
                                    }
                                }

                                // Pulsante Elimina Categoria
                                if (onDeleteCategory != null) {
                                    item {
                                        Button(
                                            onClick = { showDeleteCatConfirm = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.15f), contentColor = ExpenseRed),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f))
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Elimina Categoria", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CategorySheetMode.EDIT_SUBCATEGORY -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(bottom = 12.dp)
                        ) {
                            // Top Bar: Indietro, Titolo "Modifica Sottocategoria", e Salva
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { sheetMode = CategorySheetMode.EDIT_CATEGORY },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Indietro",
                                            tint = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Modifica Sottocategoria",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Button(
                                    onClick = {
                                        selectedSubForEdit?.let { targetSub ->
                                            if (editSubName.isNotBlank()) {
                                                onUpdateSubcategory(
                                                    targetSub.copy(
                                                        name = editSubName.trim(),
                                                        iconName = editSubIcon,
                                                        isArchived = editSubArchived
                                                    )
                                                )
                                                sheetMode = CategorySheetMode.EDIT_CATEGORY
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = categoryColor, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text("Salva", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 520.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        color = SurfaceElevated,
                                        border = BorderStroke(1.dp, SurfaceBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Nome",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                BasicTextField(
                                                    value = editSubName,
                                                    onValueChange = { editSubName = it },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        color = TextPrimary,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    cursorBrush = SolidColor(categoryColor),
                                                    decorationBox = { inner ->
                                                        if (editSubName.isEmpty()) {
                                                            Text("Nome sottocategoria...", fontSize = 18.sp, color = TextMuted)
                                                        }
                                                        inner()
                                                    }
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(categoryColor)
                                                    .clickable { showSubIconPicker = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIconByName(editSubIcon),
                                                    contentDescription = "Cambia icona sottocategoria",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Switch Archivia Sottocategoria
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = SurfaceElevated,
                                        border = BorderStroke(1.dp, SurfaceBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Archivia Sottocategoria", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                Text("Nascondi dalle scelte rapide", fontSize = 11.sp, color = TextMuted)
                                            }
                                            Switch(
                                                checked = editSubArchived,
                                                onCheckedChange = { editSubArchived = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = categoryColor)
                                            )
                                        }
                                    }
                                }

                                // Elimina Sottocategoria
                                item {
                                    Button(
                                        onClick = {
                                            selectedSubForEdit?.let {
                                                onDeleteSubcategory(it)
                                                sheetMode = CategorySheetMode.EDIT_CATEGORY
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.15f), contentColor = ExpenseRed),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f))
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Elimina Sottocategoria", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Selezione Icona e Colore Categoria
    if (showCatIconColorPicker) {
        IconAndColorPickerDialog(
            initialIcon = catIcon,
            initialColorHex = catColorHex,
            onDismiss = { showCatIconColorPicker = false },
            onConfirm = { icon, colorHex ->
                catIcon = icon
                catColorHex = colorHex
                showCatIconColorPicker = false
            }
        )
    }

    // Dialog Selezione Icona Sottocategoria
    if (showSubIconPicker) {
        IconAndColorPickerDialog(
            initialIcon = editSubIcon,
            initialColorHex = catColorHex,
            onDismiss = { showSubIconPicker = false },
            onConfirm = { icon, _ ->
                editSubIcon = icon
                showSubIconPicker = false
            }
        )
    }

    // Dialog Creazione Nuova Sottocategoria
    if (showAddSubDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Nuova Sottocategoria", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSubName,
                        onValueChange = { newSubName = it },
                        label = { Text("Nome Sottocategoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = categoryColor,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Icona:", fontSize = 13.sp, color = TextSecondary)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(categoryColor.copy(alpha = 0.20f))
                                .border(1.dp, categoryColor, RoundedCornerShape(10.dp))
                                .clickable { showNewSubIconPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIconByName(newSubIcon),
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubName.isNotBlank()) {
                            onAddSubcategory(newSubName.trim(), newSubIcon)
                            showAddSubDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = categoryColor, contentColor = Color.White)
                ) {
                    Text("Aggiungi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )

        if (showNewSubIconPicker) {
            IconAndColorPickerDialog(
                initialIcon = newSubIcon,
                initialColorHex = catColorHex,
                onDismiss = { showNewSubIconPicker = false },
                onConfirm = { icon, _ ->
                    newSubIcon = icon
                    showNewSubIconPicker = false
                }
            )
        }
    }

    // Dialog Conferma Eliminazione Categoria
    if (showDeleteCatConfirm && onDeleteCategory != null) {
        AlertDialog(
            onDismissRequest = { showDeleteCatConfirm = false },
            containerColor = SurfaceElevated,
            title = { Text("Eliminare la categoria?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Sei sicuro di voler eliminare '${category.name}'? Le transazioni associate rimarranno ma non avranno più una categoria assegnata.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteCatConfirm = false
                        onDeleteCategory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White)
                ) {
                    Text("Elimina", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCatConfirm = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun RoundCategoryActionButton(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.16f))
                .border(1.dp, iconColor.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
