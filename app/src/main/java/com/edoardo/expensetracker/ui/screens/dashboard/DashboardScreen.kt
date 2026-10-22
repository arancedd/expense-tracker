package com.edoardo.expensetracker.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.components.CalendarPeriodDialog
import com.edoardo.expensetracker.ui.components.CategoryCircleItem
import com.edoardo.expensetracker.ui.components.FinTechHeaderCard
import com.edoardo.expensetracker.ui.components.OneMoneyDonutChart
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.screens.categories.CategoryActionBottomSheet
import com.edoardo.expensetracker.ui.screens.categories.EditCategorySheet
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCategoryClick: (Long, TransactionType) -> Unit,
    onAccountsClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val allCategoriesWithSubs by viewModel.allCategoriesWithSubs.collectAsState(initial = emptyList())

    // Disattiva automaticamente modalità modifica quando si naviga via
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setEditMode(false)
        }
    }

    val categories = state.categoriesWithSpending
    val isExpense = state.currentType == TransactionType.EXPENSE

    // Se in modalità modifica, aggiungi il placeholder "Aggiungi" subito dopo l'ultima categoria
    val allItems = if (state.isEditMode) {
        categories + OneMoneyCategoryItem(
            categoryId = -1L,
            name = "Aggiungi",
            colorHex = "#10B981",
            iconName = "add",
            totalSpent = 0.0
        )
    } else {
        categories
    }

    val topMax = if (allItems.size <= 4) 2 else 4
    val midLeftMax = if (allItems.size <= 4) 1 else 2
    val midRightMax = if (allItems.size <= 4) 1 else 2

    val topRow = allItems.take(topMax)
    val middleLeft = allItems.drop(topMax).take(midLeftMax)
    val middleRight = allItems.drop(topMax + midLeftMax).take(midRightMax)
    val bottomRow = if (allItems.size <= 4) emptyList() else allItems.drop(topMax + midLeftMax + midRightMax).take(4)
    val remaining = if (allItems.size <= 4) emptyList() else allItems.drop(topMax + midLeftMax + midRightMax + 4)

    var selectedCategoryForAction by remember { mutableStateOf<OneMoneyCategoryItem?>(null) }
    var lastActionCategory by remember { mutableStateOf<OneMoneyCategoryItem?>(null) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }
    var categoryForBudgetDialog by remember { mutableStateOf<OneMoneyCategoryItem?>(null) }
    var budgetInputAmount by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val canScrollContent by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.size < totalItems) {
                true
            } else if (visibleItems.isNotEmpty()) {
                val lastItem = visibleItems.last()
                (lastItem.offset + lastItem.size) - layoutInfo.viewportEndOffset > 100
            } else {
                false
            }
        }
    }
    val isScrolled by remember {
        derivedStateOf {
            canScrollContent && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 30)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // --- 1. Header fisso in primo piano (Card Saldo che si rimpicciolisce + Pillola Calendario) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            FinTechHeaderCard(
                badgeIcon = Icons.Default.AccountBalanceWallet,
                badgeTitle = "SALDO",
                onBadgeClick = onAccountsClick,
                trailingTopContent = {
                    // Pulsante Modifica Categorie In-Place
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (state.isEditMode) PrimaryMint.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (state.isEditMode) PrimaryMint else SurfaceBorder,
                                RoundedCornerShape(11.dp)
                            )
                            .clickable { viewModel.toggleEditMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = state.isEditMode,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                            label = "edit_icon"
                        ) { isEdit ->
                            Icon(
                                imageVector = if (isEdit) Icons.Default.Check else Icons.Default.Tune,
                                contentDescription = "Modifica Categorie",
                                tint = if (isEdit) PrimaryMint else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                middleContent = {
                    Text(
                        text = "€ ${String.format(Locale.ITALY, "%,.2f", state.totalWealth)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )
                },
                bottomContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isPositiveCashflow = state.netCashflow >= 0
                        val cashflowColor = if (isPositiveCashflow) PrimaryMint else ExpenseRed

                        // Chip 1: Risparmio / Cashflow Netto del Periodo
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = cashflowColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, cashflowColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(cashflowColor.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPositiveCashflow) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = cashflowColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isPositiveCashflow) "Risparmio" else "Deficit",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${if (isPositiveCashflow) "+" else ""}€ ${String.format(Locale.ITALY, "%,.2f", state.netCashflow)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cashflowColor
                                    )
                                }
                            }
                        }

                        // Chip 2: Media Spesa al giorno nel Periodo
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SecondaryViolet.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = SecondaryViolet,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Media / Giorno",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "€ ${String.format(Locale.ITALY, "%.2f", state.dailyExpenseAverage)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                },
                bottomContentVisible = !isScrolled
            )

            // --- 2. Selettore Periodo Minimale con Pillola Data Calendario ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.previousPeriod() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Periodo precedente",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Surface(
                    onClick = {
                        if (state.isCustomDayOrRange) {
                            viewModel.resetToDefaultView()
                        } else {
                            viewModel.toggleCalendarPicker()
                        }
                    },
                    color = if (state.isCustomDayOrRange) PrimaryMint.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (state.isCustomDayOrRange) PrimaryMint.copy(alpha = 0.5f) else SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = PrimaryMint,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.periodLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = if (state.isCustomDayOrRange) PrimaryMint else TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (state.isCustomDayOrRange) Icons.Default.Close else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (state.isCustomDayOrRange) "Torna a Oggi" else "Apri Calendario",
                            tint = if (state.isCustomDayOrRange) PrimaryMint else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.nextPeriod() },
                    enabled = state.canGoForward,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Periodo successivo",
                        tint = if (state.canGoForward) TextSecondary else TextMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // --- Contenuto Scorrevole sotto l'Header fisso ---
        LazyColumn(
            state = listState,
            userScrollEnabled = canScrollContent,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

        // --- 3. Categorie Superiori (Altezza standard 98dp per leggibilità perfetta) ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(98.dp),
                contentAlignment = Alignment.Center
            ) {
                if (topRow.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        topRow.forEach { cat ->
                            RenderCategoryCircle(
                                cat = cat,
                                isEditMode = state.isEditMode,
                                currentType = state.currentType,
                                onOpenEdit = { viewModel.openEditCategory(it) },
                                onCategoryClick = onCategoryClick,
                                onLongClick = { selectedCategoryForAction = it },
                                onAddNewClick = { showCreateCategorySheet = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Area Centrale: Cat a Sinistra | Cerchio Donut Diviso | Cat a Destra ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sinistra
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        middleLeft.forEach { cat ->
                            RenderCategoryCircle(
                                cat = cat,
                                isEditMode = state.isEditMode,
                                currentType = state.currentType,
                                onOpenEdit = { viewModel.openEditCategory(it) },
                                onCategoryClick = onCategoryClick,
                                onLongClick = { selectedCategoryForAction = it },
                                onAddNewClick = { showCreateCategorySheet = true }
                            )
                        }
                    }
                }

                // Donut Chart sottile con anelli divisi e tocco per switch tra Spese / Entrate
                OneMoneyDonutChart(
                    categoryStats = state.categoryStats,
                    totalAmount = if (isExpense) state.totalExpense else state.totalIncome,
                    secondaryAmount = if (isExpense) state.totalIncome else state.totalExpense,
                    currentType = state.currentType,
                    onRingClick = { viewModel.toggleTransactionType() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Destra
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        middleRight.forEach { cat ->
                            RenderCategoryCircle(
                                cat = cat,
                                isEditMode = state.isEditMode,
                                currentType = state.currentType,
                                onOpenEdit = { viewModel.openEditCategory(it) },
                                onCategoryClick = onCategoryClick,
                                onLongClick = { selectedCategoryForAction = it },
                                onAddNewClick = { showCreateCategorySheet = true }
                            )
                        }
                    }
                }
            }
        }

        // --- 5. Categorie Inferiori ---
        if (bottomRow.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(98.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        bottomRow.forEach { cat ->
                            RenderCategoryCircle(
                                cat = cat,
                                isEditMode = state.isEditMode,
                                currentType = state.currentType,
                                onOpenEdit = { viewModel.openEditCategory(it) },
                                onCategoryClick = onCategoryClick,
                                onLongClick = { selectedCategoryForAction = it },
                                onAddNewClick = { showCreateCategorySheet = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // --- 6. Categorie Extra (suddivise in righe da 4) ---
        if (remaining.isNotEmpty()) {
            remaining.chunked(4).forEach { chunk ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        chunk.forEach { cat ->
                            RenderCategoryCircle(
                                cat = cat,
                                isEditMode = state.isEditMode,
                                currentType = state.currentType,
                                onOpenEdit = { viewModel.openEditCategory(it) },
                                onCategoryClick = onCategoryClick,
                                onLongClick = { selectedCategoryForAction = it },
                                onAddNewClick = { showCreateCategorySheet = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Riempi spazi vuoti per mantenere l'allineamento a 4 colonne
                        repeat(4 - chunk.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            // Spaziatore inferiore ridotto: il padding per la navbar è già fornito dallo Scaffold
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    }

    // --- Bottom Sheet Azioni Categoria (Aperta con Long-Press sulla categoria) ---
    val activeCatAction = selectedCategoryForAction
    if (activeCatAction != null) {
        val matchedCatWithSubs = allCategoriesWithSubs.find { it.category.id == activeCatAction.categoryId }
        if (matchedCatWithSubs != null) {
            val catTransactions by viewModel.getTransactionsForCategory(activeCatAction.categoryId)
                .collectAsState(initial = emptyList())

            CategoryActionBottomSheet(
                categoryWithSubs = matchedCatWithSubs,
                categorySpent = activeCatAction.totalSpent,
                periodTotal = if (isExpense) state.totalExpense else state.totalIncome,
                periodLabel = state.periodLabel,
                transactions = catTransactions,
                onDismiss = { selectedCategoryForAction = null },
                onSaveCategory = { name, color, icon, archived ->
                    viewModel.saveCategoryEdits(
                        categoryId = matchedCatWithSubs.category.id,
                        newName = name,
                        newColorHex = color,
                        newIconName = icon,
                        isArchived = archived
                    )
                },
                onAddSubcategory = { subName, subIcon ->
                    viewModel.addSubcategory(
                        categoryId = matchedCatWithSubs.category.id,
                        name = subName,
                        iconName = subIcon
                    )
                },
                onToggleSubcategoryArchived = { sub ->
                    viewModel.toggleSubcategoryArchived(sub)
                },
                onDeleteSubcategory = { sub ->
                    viewModel.deleteSubcategory(sub)
                },
                onUpdateSubcategory = { sub ->
                    viewModel.updateSubcategory(sub)
                },
                onDeleteCategory = {
                    viewModel.deleteCategory(matchedCatWithSubs.category.id)
                    selectedCategoryForAction = null
                },
                onBudgetClick = {
                    categoryForBudgetDialog = activeCatAction
                    budgetInputAmount = ""
                    selectedCategoryForAction = null
                },
                onTransactionClick = { _ -> }
            )
        }
    }

    // --- Bottom Sheet Modifica Categoria (OneMoney/Ivy Style) ---
    if (state.isEditSheetOpen && state.selectedCategoryForEdit != null) {
        EditCategorySheet(
            categoryWithSubs = state.selectedCategoryForEdit,
            onDismiss = {
                viewModel.closeEditCategory()
                if (lastActionCategory != null) {
                    selectedCategoryForAction = lastActionCategory
                    lastActionCategory = null
                }
            },
            onSave = { name, color, icon, archived ->
                viewModel.saveCategoryEdits(
                    categoryId = state.selectedCategoryForEdit!!.category.id,
                    newName = name,
                    newColorHex = color,
                    newIconName = icon,
                    isArchived = archived
                )
                if (lastActionCategory != null) {
                    selectedCategoryForAction = lastActionCategory
                    lastActionCategory = null
                }
            },
            onAddSubcategory = { subName, subIcon ->
                viewModel.addSubcategory(
                    categoryId = state.selectedCategoryForEdit!!.category.id,
                    name = subName,
                    iconName = subIcon
                )
            },
            onToggleSubcategoryArchived = { sub ->
                viewModel.toggleSubcategoryArchived(sub)
            },
            onUpdateSubcategory = { sub ->
                viewModel.updateSubcategory(sub)
            },
            onDeleteSubcategory = { sub ->
                viewModel.deleteSubcategory(sub)
            },
            onDeleteCategory = {
                lastActionCategory = null
                viewModel.deleteCategory(state.selectedCategoryForEdit!!.category.id)
            }
        )
    }

    // --- Bottom Sheet Nuova Categoria ---
    if (showCreateCategorySheet) {
        EditCategorySheet(
            categoryWithSubs = null,
            onDismiss = { showCreateCategorySheet = false },
            onSave = { name, color, icon, _ ->
                viewModel.createCategory(
                    name = name,
                    colorHex = color,
                    iconName = icon,
                    type = state.currentType
                )
                showCreateCategorySheet = false
            },
            onAddSubcategory = { _, _ -> },
            onToggleSubcategoryArchived = {},
            onDeleteSubcategory = {}
        )
    }

    // --- Dialog Imposta Budget al volo ---
    val activeBudgetCat = categoryForBudgetDialog
    if (activeBudgetCat != null) {
        AlertDialog(
            onDismissRequest = { categoryForBudgetDialog = null },
            containerColor = SurfaceElevated,
            title = {
                Text("Imposta Budget per ${activeBudgetCat.name}", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = budgetInputAmount,
                    onValueChange = { budgetInputAmount = it },
                    label = { Text("Budget Mensile (€)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryMint,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = budgetInputAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.setCategoryBudget(activeBudgetCat.categoryId, amount)
                        }
                        categoryForBudgetDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = Color.Black)
                ) {
                    Text("Salva", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryForBudgetDialog = null }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }

    // --- Dialog Calendario & Periodo ---
    if (state.isCalendarPickerOpen) {
        CalendarPeriodDialog(
            currentPeriod = state.selectedPeriod,
            currentDateMillis = state.currentDateMillis,
            selectedRangeStartMillis = state.selectedRangeStartMillis,
            selectedRangeEndMillis = state.selectedRangeEndMillis,
            onSelectPeriod = { viewModel.setPeriod(it) },
            onSelectDay = { viewModel.selectDateFromCalendar(it) },
            onSelectRange = { start, end -> viewModel.selectDateRange(start, end) },
            onResetToDefault = { viewModel.resetToDefaultView() },
            onDismissRequest = { viewModel.closeCalendarPicker() },
            defaultActionLabel = "Torna a Oggi"
        )
    }
}

@Composable
private fun RenderCategoryCircle(
    cat: OneMoneyCategoryItem,
    isEditMode: Boolean,
    currentType: TransactionType,
    onOpenEdit: (Long) -> Unit,
    onCategoryClick: (Long, TransactionType) -> Unit,
    onLongClick: (OneMoneyCategoryItem) -> Unit,
    onAddNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (cat.categoryId == -1L) {
        CategoryCircleItem(
            categoryName = "Aggiungi",
            totalAmount = 0.0,
            colorHex = "#10B981",
            icon = Icons.Default.Add,
            isEditMode = true,
            onClick = onAddNewClick,
            modifier = modifier
        )
    } else {
        CategoryCircleItem(
            categoryName = cat.name,
            totalAmount = cat.totalSpent,
            budgetLimit = cat.budgetLimit,
            hasBudget = cat.hasBudget,
            colorHex = cat.colorHex,
            icon = getCategoryIconByName(cat.iconName),
            isEditMode = isEditMode,
            onClick = {
                if (isEditMode) {
                    onOpenEdit(cat.categoryId)
                } else {
                    onCategoryClick(cat.categoryId, currentType)
                }
            },
            onLongClick = {
                if (!isEditMode) {
                    onLongClick(cat)
                }
            },
            modifier = modifier
        )
    }
}
