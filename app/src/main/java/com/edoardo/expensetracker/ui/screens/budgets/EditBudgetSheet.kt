package com.edoardo.expensetracker.ui.screens.budgets

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.CategoryWithSubcategories
import com.edoardo.expensetracker.data.local.entity.*
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

data class CategoryBudgetDraft(
    val categoryId: Long,
    val isPercentage: Boolean = false,
    val valueInput: String = "",
    val hasSubcategoriesBudget: Boolean = false
)

data class SubcategoryBudgetDraft(
    val subcategoryId: Long,
    val categoryId: Long,
    val isPercentage: Boolean = false,
    val valueInput: String = ""
)

data class SavingsBudgetDraft(
    val accountId: Long,
    val isPercentage: Boolean = false,
    val valueInput: String = ""
)

private enum class SheetView {
    MAIN,
    PICKER_INCOME_CATEGORY,
    PICKER_EXPENSE_CATEGORY,
    PICKER_SAVINGS_ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditBudgetSheet(
    incomeCategories: List<CategoryWithSubcategories>,
    expenseCategories: List<CategoryWithSubcategories>,
    savingsAccounts: List<AccountEntity>,
    initialExpectedIncomes: List<ExpectedIncomeEntity>,
    initialBudgets: List<BudgetEntity>,
    initialSubcategoryBudgets: List<SubcategoryBudgetEntity>,
    initialSavingsBudgets: List<SavingsBudgetEntity>,
    onDismiss: () -> Unit,
    onSave: (
        expectedIncomes: List<ExpectedIncomeEntity>,
        budgets: List<BudgetEntity>,
        subcategoryBudgets: List<SubcategoryBudgetEntity>,
        savingsBudgets: List<SavingsBudgetEntity>
    ) -> Unit
) {
    var currentView by remember { mutableStateOf(SheetView.MAIN) }

    // Categorie Entrate selezionate
    val selectedIncomeCategoryIds = remember {
        mutableStateListOf<Long>().apply {
            val fromSaved = initialExpectedIncomes.filter { it.expectedAmount > 0 }.map { it.categoryId }
            addAll(fromSaved)
        }
    }

    // Input per entrate: categoryId -> testo
    val expectedIncomeInputs = remember {
        mutableStateMapOf<Long, String>().apply {
            initialExpectedIncomes.forEach {
                val str = if (it.expectedAmount > 0) {
                    if (it.expectedAmount % 1.0 == 0.0) it.expectedAmount.toInt().toString()
                    else String.format(Locale.US, "%.2f", it.expectedAmount)
                } else ""
                put(it.categoryId, str)
            }
        }
    }

    // Categorie Uscite selezionate
    val selectedExpenseCategoryIds = remember {
        mutableStateListOf<Long>().apply {
            val fromSaved = initialBudgets.filter { it.amountLimit > 0 }.map { it.categoryId }
            addAll(fromSaved)
        }
    }

    // Draft budget per categoria
    val expenseBudgetDrafts = remember {
        mutableStateMapOf<Long, CategoryBudgetDraft>().apply {
            initialBudgets.forEach { budget ->
                val str = if (budget.isPercentage) {
                    if (budget.percentageValue % 1.0 == 0.0) budget.percentageValue.toInt().toString()
                    else String.format(Locale.US, "%.1f", budget.percentageValue)
                } else {
                    if (budget.amountLimit % 1.0 == 0.0) budget.amountLimit.toInt().toString()
                    else String.format(Locale.US, "%.2f", budget.amountLimit)
                }
                put(
                    budget.categoryId,
                    CategoryBudgetDraft(
                        categoryId = budget.categoryId,
                        isPercentage = budget.isPercentage,
                        valueInput = str,
                        hasSubcategoriesBudget = budget.hasSubcategoriesBudget
                    )
                )
            }
        }
    }

    // Draft budget per sottocategorie: subcategoryId -> SubcategoryBudgetDraft
    val subcategoryBudgetDrafts = remember {
        mutableStateMapOf<Long, SubcategoryBudgetDraft>().apply {
            initialSubcategoryBudgets.forEach { subBudget ->
                val str = if (subBudget.isPercentage) {
                    if (subBudget.percentageValue % 1.0 == 0.0) subBudget.percentageValue.toInt().toString()
                    else String.format(Locale.US, "%.1f", subBudget.percentageValue)
                } else {
                    if (subBudget.amountLimit % 1.0 == 0.0) subBudget.amountLimit.toInt().toString()
                    else String.format(Locale.US, "%.2f", subBudget.amountLimit)
                }
                put(
                    subBudget.subcategoryId,
                    SubcategoryBudgetDraft(
                        subcategoryId = subBudget.subcategoryId,
                        categoryId = subBudget.categoryId,
                        isPercentage = subBudget.isPercentage,
                        valueInput = str
                    )
                )
            }
        }
    }

    // Conti Risparmio selezionati
    val selectedSavingsAccountIds = remember {
        mutableStateListOf<Long>().apply {
            val fromSaved = initialSavingsBudgets.filter { it.amountLimit > 0 }.map { it.accountId }
            addAll(fromSaved)
        }
    }

    // Draft budget per conti risparmio: accountId -> SavingsBudgetDraft
    val savingsBudgetDrafts = remember {
        mutableStateMapOf<Long, SavingsBudgetDraft>().apply {
            initialSavingsBudgets.forEach { sb ->
                val str = if (sb.isPercentage) {
                    if (sb.percentageValue % 1.0 == 0.0) sb.percentageValue.toInt().toString()
                    else String.format(Locale.US, "%.1f", sb.percentageValue)
                } else {
                    if (sb.amountLimit % 1.0 == 0.0) sb.amountLimit.toInt().toString()
                    else String.format(Locale.US, "%.2f", sb.amountLimit)
                }
                put(
                    sb.accountId,
                    SavingsBudgetDraft(
                        accountId = sb.accountId,
                        isPercentage = sb.isPercentage,
                        valueInput = str
                    )
                )
            }
        }
    }

    // Calcolo Totale Entrate Previste
    val totalExpectedIncome = remember(expectedIncomeInputs.values.toList(), selectedIncomeCategoryIds.toList()) {
        selectedIncomeCategoryIds.sumOf { catId ->
            val str = expectedIncomeInputs[catId]?.replace(',', '.')?.trim() ?: ""
            str.toDoubleOrNull() ?: 0.0
        }
    }

    // Calcolo Totale Allocato Spese (inclusi sottocategorie e conti risparmio)
    val totalExpenseAllocated = remember(
        totalExpectedIncome,
        expenseBudgetDrafts.values.toList(),
        subcategoryBudgetDrafts.values.toList(),
        selectedExpenseCategoryIds.toList()
    ) {
        selectedExpenseCategoryIds.sumOf { catId ->
            val draft = expenseBudgetDrafts[catId]
            if (draft?.hasSubcategoriesBudget == true) {
                // Somma delle sottocategorie di questa categoria
                val catWithSubs = expenseCategories.find { it.category.id == catId }
                val subsSum = catWithSubs?.subcategories?.sumOf { sub ->
                    val subDraft = subcategoryBudgetDrafts[sub.id]
                    val subNum = subDraft?.valueInput?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                    if (subNum <= 0.0) 0.0
                    else if (subDraft?.isPercentage == true) (subNum / 100.0) * totalExpectedIncome
                    else subNum
                } ?: 0.0
                subsSum
            } else {
                val valNum = draft?.valueInput?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                if (valNum <= 0.0) 0.0
                else if (draft?.isPercentage == true) (valNum / 100.0) * totalExpectedIncome
                else valNum
            }
        }
    }

    // Calcolo Totale Allocato Risparmi
    val totalSavingsAllocated = remember(
        totalExpectedIncome,
        savingsBudgetDrafts.values.toList(),
        selectedSavingsAccountIds.toList()
    ) {
        selectedSavingsAccountIds.sumOf { accId ->
            val draft = savingsBudgetDrafts[accId]
            val valNum = draft?.valueInput?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            if (valNum <= 0.0) 0.0
            else if (draft?.isPercentage == true) (valNum / 100.0) * totalExpectedIncome
            else valNum
        }
    }

    val totalAllocated = totalExpenseAllocated + totalSavingsAllocated
    val remainingBudget = totalExpectedIncome - totalAllocated
    val remainingPercent = if (totalExpectedIncome > 0) {
        ((remainingBudget / totalExpectedIncome) * 100.0).coerceIn(-999.0, 100.0)
    } else 0.0
    val allocatedPercent = if (totalExpectedIncome > 0) {
        ((totalAllocated / totalExpectedIncome) * 100.0).coerceAtLeast(0.0)
    } else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark,
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
        AnimatedContent(
            targetState = currentView,
            transitionSpec = {
                if (targetState == SheetView.MAIN) {
                    (slideInHorizontally(tween(260)) { -it / 2 } + fadeIn(tween(240)))
                        .togetherWith(slideOutHorizontally(tween(240)) { it } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(260)) { it } + fadeIn(tween(240)))
                        .togetherWith(slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(200)))
                }
            },
            label = "budget_sheet_view_switcher"
        ) { view ->
            when (view) {
                SheetView.MAIN -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        ) {
                            // Top Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Indietro",
                                            tint = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pianificazione Budget",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Button(
                                    onClick = {
                                        // Entrate da salvare
                                        val incomesToSave = selectedIncomeCategoryIds.mapNotNull { catId ->
                                            val str = expectedIncomeInputs[catId]?.replace(',', '.')?.trim() ?: ""
                                            val amt = str.toDoubleOrNull() ?: 0.0
                                            if (amt > 0) {
                                                ExpectedIncomeEntity(categoryId = catId, expectedAmount = amt)
                                            } else null
                                        }

                                        // Sottocategorie da salvare
                                        val subBudgetsToSave = mutableListOf<SubcategoryBudgetEntity>()

                                        // Budget categorie da salvare
                                        val budgetsToSave = selectedExpenseCategoryIds.mapNotNull { catId ->
                                            val draft = expenseBudgetDrafts[catId]
                                            val catWithSubs = expenseCategories.find { it.category.id == catId }

                                            if (draft?.hasSubcategoriesBudget == true && catWithSubs != null) {
                                                // Raccogli budget sottocategorie
                                                var catLimit = 0.0
                                                catWithSubs.subcategories.forEach { sub ->
                                                    val subDraft = subcategoryBudgetDrafts[sub.id]
                                                    val subVal = subDraft?.valueInput?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                                                    if (subVal > 0) {
                                                        val subLimit = if (subDraft?.isPercentage == true) {
                                                            (subVal / 100.0) * totalExpectedIncome
                                                        } else subVal

                                                        subBudgetsToSave.add(
                                                            SubcategoryBudgetEntity(
                                                                subcategoryId = sub.id,
                                                                categoryId = catId,
                                                                amountLimit = subLimit,
                                                                isPercentage = subDraft?.isPercentage ?: false,
                                                                percentageValue = if (subDraft?.isPercentage == true) subVal else {
                                                                    if (totalExpectedIncome > 0) (subVal / totalExpectedIncome) * 100.0 else 0.0
                                                                }
                                                            )
                                                        )
                                                        catLimit += subLimit
                                                    }
                                                }

                                                if (catLimit > 0) {
                                                    BudgetEntity(
                                                        categoryId = catId,
                                                        amountLimit = catLimit,
                                                        isPercentage = false,
                                                        percentageValue = if (totalExpectedIncome > 0) (catLimit / totalExpectedIncome) * 100.0 else 0.0,
                                                        hasSubcategoriesBudget = true
                                                    )
                                                } else null
                                            } else {
                                                val valNum = draft?.valueInput?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                                                if (valNum > 0) {
                                                    val limit = if (draft?.isPercentage == true) {
                                                        (valNum / 100.0) * totalExpectedIncome
                                                    } else valNum

                                                    BudgetEntity(
                                                        categoryId = catId,
                                                        amountLimit = limit,
                                                        isPercentage = draft?.isPercentage ?: false,
                                                        percentageValue = if (draft?.isPercentage == true) valNum else {
                                                            if (totalExpectedIncome > 0) (valNum / totalExpectedIncome) * 100.0 else 0.0
                                                        },
                                                        hasSubcategoriesBudget = false
                                                    )
                                                } else null
                                            }
                                        }

                                        // Conti Risparmio da salvare
                                        val savingsToSave = selectedSavingsAccountIds.mapNotNull { accId ->
                                            val draft = savingsBudgetDrafts[accId]
                                            val valNum = draft?.valueInput?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                                            if (valNum > 0) {
                                                val limit = if (draft?.isPercentage == true) {
                                                    (valNum / 100.0) * totalExpectedIncome
                                                } else valNum

                                                SavingsBudgetEntity(
                                                    accountId = accId,
                                                    amountLimit = limit,
                                                    isPercentage = draft?.isPercentage ?: false,
                                                    percentageValue = if (draft?.isPercentage == true) valNum else {
                                                        if (totalExpectedIncome > 0) (valNum / totalExpectedIncome) * 100.0 else 0.0
                                                    }
                                                )
                                            } else null
                                        }

                                        onSave(incomesToSave, budgetsToSave, subBudgetsToSave, savingsToSave)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text("Salva", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 150.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // --- SEZIONE 1: ENTRATE PREVISTE ---
                                item {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryMint.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("1", color = PrimaryMint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Entrate Previste",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Aggiungi le fonti di entrata che ti aspetti per determinare il budget mensile.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                // Categorie Entrate Aggiunte
                                items(selectedIncomeCategoryIds) { catId ->
                                    val catWithSubs = incomeCategories.find { it.category.id == catId }
                                    if (catWithSubs != null) {
                                        val cat = catWithSubs.category
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(cat.colorHex))
                                        } catch (_: Exception) { PrimaryMint }
                                        val currentVal = expectedIncomeInputs[cat.id] ?: ""

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
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(11.dp))
                                                            .background(catColor.copy(alpha = 0.18f))
                                                            .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(11.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = getCategoryIconByName(cat.iconName),
                                                            contentDescription = null,
                                                            tint = catColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = cat.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp,
                                                        color = TextPrimary
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    BudgetNumericInputField(
                                                        value = currentVal,
                                                        onValueChange = { expectedIncomeInputs[cat.id] = it },
                                                        prefix = "€ ",
                                                        placeholder = "0",
                                                        modifier = Modifier.width(115.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = {
                                                            selectedIncomeCategoryIds.remove(catId)
                                                            expectedIncomeInputs.remove(catId)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Rimuovi",
                                                            tint = TextSecondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Tasto '+' con le dimensioni esatte della card (finché ci sono categorie disponibili)
                                if (selectedIncomeCategoryIds.size < incomeCategories.size) {
                                    item {
                                        BudgetAddPlaceholderCard(
                                            onClick = { currentView = SheetView.PICKER_INCOME_CATEGORY }
                                        )
                                    }
                                }

                                // Card Totale Entrate
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = PrimaryMint.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Totale Entrate Previste",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "€ ${String.format(Locale.ITALY, "%,.2f", totalExpectedIncome)}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PrimaryMint
                                            )
                                        }
                                    }
                                }

                                // --- SEZIONE 2: BUDGET CATEGORIE SPESE ---
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (totalExpectedIncome > 0) PrimaryMint.copy(alpha = 0.2f) else SurfaceBorder),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "2",
                                                    color = if (totalExpectedIncome > 0) PrimaryMint else TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Budget Categorie Spese",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tieni premuto su una categoria per gestire i budget delle singole sottocategorie.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                if (totalExpectedIncome <= 0) {
                                    item {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = SurfaceElevated,
                                            border = BorderStroke(1.dp, SurfaceBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Imposta prima le entrate previste",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Aggiungi almeno un'entrata nella sezione 1 per iniziare ad allocare le spese.",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Lista Categorie Spese Selezionate
                                    items(selectedExpenseCategoryIds) { catId ->
                                        val catWithSubs = expenseCategories.find { it.category.id == catId }
                                        if (catWithSubs != null) {
                                            val cat = catWithSubs.category
                                            val catColor = try {
                                                Color(android.graphics.Color.parseColor(cat.colorHex))
                                            } catch (_: Exception) { PrimaryMint }
                                            val draft = expenseBudgetDrafts[cat.id] ?: CategoryBudgetDraft(categoryId = cat.id)
                                            val isSubExpanded = draft.hasSubcategoriesBudget

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .combinedClickable(
                                                        onClick = {},
                                                        onLongClick = {
                                                            // Toggle espansione sottocategorie
                                                            if (catWithSubs.subcategories.isNotEmpty()) {
                                                                expenseBudgetDrafts[cat.id] = draft.copy(
                                                                    hasSubcategoriesBudget = !isSubExpanded
                                                                )
                                                            }
                                                        }
                                                    ),
                                                shape = RoundedCornerShape(16.dp),
                                                color = SurfaceElevated,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSubExpanded) catColor.copy(alpha = 0.5f) else SurfaceBorder
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    // Riga Categoria
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .clip(RoundedCornerShape(11.dp))
                                                                    .background(catColor.copy(alpha = 0.18f))
                                                                    .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(11.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = getCategoryIconByName(cat.iconName),
                                                                    contentDescription = null,
                                                                    tint = catColor,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Column {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(
                                                                        text = cat.name,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        fontSize = 14.sp,
                                                                        color = TextPrimary
                                                                    )
                                                                    if (isSubExpanded) {
                                                                        Spacer(modifier = Modifier.width(6.dp))
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(6.dp))
                                                                                .background(PrimaryMint.copy(alpha = 0.15f))
                                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                        ) {
                                                                            Text(
                                                                                text = "Sottocategorie",
                                                                                fontSize = 9.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = PrimaryMint
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                if (catWithSubs.subcategories.isNotEmpty()) {
                                                                    Text(
                                                                        text = if (isSubExpanded) "Tieni premuto per tornare a budget categoria" else "Tieni premuto per sottocategorie",
                                                                        fontSize = 10.sp,
                                                                        color = TextSecondary
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (!isSubExpanded) {
                                                                SegmentedModeToggle(
                                                                    isPercentage = draft.isPercentage,
                                                                    onToggle = { newIsPerc ->
                                                                        if (newIsPerc != draft.isPercentage) {
                                                                            val numericVal = draft.valueInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                                            val convertedStr = if (numericVal > 0) {
                                                                                if (newIsPerc) {
                                                                                    val perc = if (totalExpectedIncome > 0) (numericVal / totalExpectedIncome) * 100.0 else 0.0
                                                                                    if (perc % 1.0 == 0.0) perc.toInt().toString() else String.format(Locale.US, "%.1f", perc)
                                                                                } else {
                                                                                    val euro = (numericVal / 100.0) * totalExpectedIncome
                                                                                    if (euro % 1.0 == 0.0) euro.toInt().toString() else String.format(Locale.US, "%.2f", euro)
                                                                                }
                                                                            } else ""
                                                                            expenseBudgetDrafts[cat.id] = draft.copy(
                                                                                isPercentage = newIsPerc,
                                                                                valueInput = convertedStr
                                                                            )
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            IconButton(
                                                                onClick = {
                                                                    selectedExpenseCategoryIds.remove(catId)
                                                                    expenseBudgetDrafts.remove(catId)
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Rimuovi",
                                                                    tint = TextSecondary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    if (!isSubExpanded) {
                                                        // Riga Normale di Budget della Categoria
                                                        val numericVal = draft.valueInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                        val calculatedEuro = if (draft.isPercentage) (numericVal / 100.0) * totalExpectedIncome else numericVal
                                                        val calculatedPerc = if (!draft.isPercentage && totalExpectedIncome > 0) (numericVal / totalExpectedIncome) * 100.0 else numericVal

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                if (numericVal > 0) {
                                                                    if (draft.isPercentage) {
                                                                        Text(
                                                                            text = "= € ${String.format(Locale.ITALY, "%,.2f", calculatedEuro)}",
                                                                            fontSize = 13.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = PrimaryMint
                                                                        )
                                                                    } else {
                                                                        Text(
                                                                            text = "${String.format(Locale.ITALY, "%.1f", calculatedPerc)}% delle entrate",
                                                                            fontSize = 12.sp,
                                                                            fontWeight = FontWeight.Medium,
                                                                            color = TextSecondary
                                                                        )
                                                                    }
                                                                } else {
                                                                    Text("Nessun budget impostato", fontSize = 12.sp, color = TextSecondary)
                                                                }
                                                            }

                                                            BudgetNumericInputField(
                                                                value = draft.valueInput,
                                                                onValueChange = { newVal ->
                                                                    expenseBudgetDrafts[cat.id] = draft.copy(valueInput = newVal)
                                                                },
                                                                prefix = if (!draft.isPercentage) "€ " else null,
                                                                suffix = if (draft.isPercentage) " %" else null,
                                                                placeholder = "0",
                                                                modifier = Modifier.width(115.dp)
                                                            )
                                                        }
                                                    } else {
                                                        // Modalità Sottocategorie: Calcolo totale + Lista sottocategorie
                                                        val subsSum = catWithSubs.subcategories.sumOf { sub ->
                                                            val subDraft = subcategoryBudgetDrafts[sub.id]
                                                            val subNum = subDraft?.valueInput?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
                                                            if (subNum <= 0.0) 0.0
                                                            else if (subDraft?.isPercentage == true) (subNum / 100.0) * totalExpectedIncome
                                                            else subNum
                                                        }

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(SurfaceDark)
                                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = "Totale Categoria (calcolato):",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = TextSecondary
                                                            )
                                                            Text(
                                                                text = "€ ${String.format(Locale.ITALY, "%,.2f", subsSum)}",
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = PrimaryMint
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(10.dp))

                                                        // Elenco Sottocategorie
                                                        catWithSubs.subcategories.forEach { sub ->
                                                            val subDraft = subcategoryBudgetDrafts[sub.id] ?: SubcategoryBudgetDraft(
                                                                subcategoryId = sub.id,
                                                                categoryId = cat.id
                                                            )
                                                            val subVal = subDraft.valueInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                            val subEuro = if (subDraft.isPercentage) (subVal / 100.0) * totalExpectedIncome else subVal

                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = sub.name,
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.Medium,
                                                                        color = TextPrimary
                                                                    )
                                                                    if (subVal > 0 && subDraft.isPercentage) {
                                                                        Text(
                                                                            text = "= € ${String.format(Locale.ITALY, "%,.2f", subEuro)}",
                                                                            fontSize = 11.sp,
                                                                            color = PrimaryMint
                                                                        )
                                                                    }
                                                                }

                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    SegmentedModeToggle(
                                                                        isPercentage = subDraft.isPercentage,
                                                                        onToggle = { newPerc ->
                                                                            subcategoryBudgetDrafts[sub.id] = subDraft.copy(isPercentage = newPerc)
                                                                        }
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    BudgetNumericInputField(
                                                                        value = subDraft.valueInput,
                                                                        onValueChange = { newVal ->
                                                                            subcategoryBudgetDrafts[sub.id] = subDraft.copy(valueInput = newVal)
                                                                        },
                                                                        prefix = if (!subDraft.isPercentage) "€ " else null,
                                                                        suffix = if (subDraft.isPercentage) " %" else null,
                                                                        placeholder = "0",
                                                                        modifier = Modifier.width(100.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Tasto '+' con le dimensioni della card per aggiungere altre categorie spesa
                                    if (selectedExpenseCategoryIds.size < expenseCategories.size) {
                                        item {
                                            BudgetAddPlaceholderCard(
                                                onClick = { currentView = SheetView.PICKER_EXPENSE_CATEGORY }
                                            )
                                        }
                                    }
                                }

                                // --- SEZIONE 3: CONTI RISPARMIO ---
                                if (savingsAccounts.isNotEmpty() && totalExpectedIncome > 0) {
                                    item {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(PrimaryMint.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("3", color = PrimaryMint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Risparmi & Accantonamenti",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Quanti soldi prevedi di trasferire ogni mese verso i tuoi conti di risparmio.",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    items(selectedSavingsAccountIds) { accId ->
                                        val account = savingsAccounts.find { it.id == accId }
                                        if (account != null) {
                                            val accColor = try {
                                                Color(android.graphics.Color.parseColor(account.colorHex))
                                            } catch (_: Exception) { PrimaryMint }
                                            val draft = savingsBudgetDrafts[accId] ?: SavingsBudgetDraft(accountId = accId)
                                            val numVal = draft.valueInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                                            val calcEuro = if (draft.isPercentage) (numVal / 100.0) * totalExpectedIncome else numVal

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                color = SurfaceElevated,
                                                border = BorderStroke(1.dp, SurfaceBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .clip(RoundedCornerShape(11.dp))
                                                                    .background(accColor.copy(alpha = 0.18f))
                                                                    .border(1.dp, accColor.copy(alpha = 0.4f), RoundedCornerShape(11.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Savings,
                                                                    contentDescription = null,
                                                                    tint = accColor,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Text(
                                                                text = account.name,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 14.sp,
                                                                color = TextPrimary
                                                            )
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            SegmentedModeToggle(
                                                                isPercentage = draft.isPercentage,
                                                                onToggle = { newPerc ->
                                                                    savingsBudgetDrafts[accId] = draft.copy(isPercentage = newPerc)
                                                                }
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            IconButton(
                                                                onClick = {
                                                                    selectedSavingsAccountIds.remove(accId)
                                                                    savingsBudgetDrafts.remove(accId)
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Rimuovi",
                                                                    tint = TextSecondary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            if (numVal > 0) {
                                                                if (draft.isPercentage) {
                                                                    Text(
                                                                        text = "= € ${String.format(Locale.ITALY, "%,.2f", calcEuro)}",
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = PrimaryMint
                                                                    )
                                                                } else {
                                                                    val perc = if (totalExpectedIncome > 0) (numVal / totalExpectedIncome) * 100.0 else 0.0
                                                                    Text(
                                                                        text = "${String.format(Locale.ITALY, "%.1f", perc)}% delle entrate",
                                                                        fontSize = 12.sp,
                                                                        color = TextSecondary
                                                                    )
                                                                }
                                                            } else {
                                                                Text("Nessun risparmio programmato", fontSize = 12.sp, color = TextSecondary)
                                                            }
                                                        }

                                                        BudgetNumericInputField(
                                                            value = draft.valueInput,
                                                            onValueChange = { newVal ->
                                                                savingsBudgetDrafts[accId] = draft.copy(valueInput = newVal)
                                                            },
                                                            prefix = if (!draft.isPercentage) "€ " else null,
                                                            suffix = if (draft.isPercentage) " %" else null,
                                                            placeholder = "0",
                                                            modifier = Modifier.width(115.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Tasto '+' per aggiungere conti risparmio
                                    if (selectedSavingsAccountIds.size < savingsAccounts.size) {
                                        item {
                                            BudgetAddPlaceholderCard(
                                                onClick = { currentView = SheetView.PICKER_SAVINGS_ACCOUNT }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- BARRA INFERIORE PERSISTENTE (STICKY FOOTER) ---
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                val isOverBudget = remainingBudget < 0
                                val progressValue = if (totalExpectedIncome > 0) {
                                    (totalAllocated / totalExpectedIncome).toFloat().coerceIn(0f, 1f)
                                } else 0f

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isOverBudget) "Budget Sforato" else "Budget Rimanente",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOverBudget) ExpenseRed else TextSecondary
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "€ ${String.format(Locale.ITALY, "%,.2f", kotlin.math.abs(remainingBudget))}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isOverBudget) ExpenseRed else PrimaryMint
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${String.format(Locale.ITALY, "%.1f", remainingPercent)}%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverBudget) ExpenseRed else PrimaryMint
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isOverBudget) ExpenseRed else PrimaryMint,
                                    trackColor = SurfaceDark
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Entrate: € ${String.format(Locale.ITALY, "%,.2f", totalExpectedIncome)}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Allocato: € ${String.format(Locale.ITALY, "%,.2f", totalAllocated)} (${String.format(Locale.ITALY, "%.1f", allocatedPercent)}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                SheetView.PICKER_INCOME_CATEGORY -> {
                    // Griglia Categorie Entrata a 4 colonne
                    val availableIncomeCats = incomeCategories.filter { it.category.id !in selectedIncomeCategoryIds }
                    CategoryGridPickerView(
                        title = "Aggiungi Categoria Entrata",
                        items = availableIncomeCats.map { catWithSubs ->
                            val cat = catWithSubs.category
                            val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { PrimaryMint }
                            PickerItem(
                                id = cat.id,
                                label = cat.name,
                                icon = getCategoryIconByName(cat.iconName),
                                color = color
                            )
                        },
                        onBack = { currentView = SheetView.MAIN },
                        onItemSelected = { catId ->
                            if (catId !in selectedIncomeCategoryIds) {
                                selectedIncomeCategoryIds.add(catId)
                            }
                            currentView = SheetView.MAIN
                        }
                    )
                }

                SheetView.PICKER_EXPENSE_CATEGORY -> {
                    // Griglia Categorie Uscita a 4 colonne
                    val availableExpenseCats = expenseCategories.filter { it.category.id !in selectedExpenseCategoryIds }
                    CategoryGridPickerView(
                        title = "Aggiungi Categoria Spesa",
                        items = availableExpenseCats.map { catWithSubs ->
                            val cat = catWithSubs.category
                            val color = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (_: Exception) { PrimaryMint }
                            PickerItem(
                                id = cat.id,
                                label = cat.name,
                                icon = getCategoryIconByName(cat.iconName),
                                color = color
                            )
                        },
                        onBack = { currentView = SheetView.MAIN },
                        onItemSelected = { catId ->
                            if (catId !in selectedExpenseCategoryIds) {
                                selectedExpenseCategoryIds.add(catId)
                            }
                            currentView = SheetView.MAIN
                        }
                    )
                }

                SheetView.PICKER_SAVINGS_ACCOUNT -> {
                    // Griglia Conti Risparmio a 4 colonne
                    val availableSavingsAccounts = savingsAccounts.filter { it.id !in selectedSavingsAccountIds }
                    CategoryGridPickerView(
                        title = "Aggiungi Conto Risparmio",
                        items = availableSavingsAccounts.map { acc ->
                            val color = try { Color(android.graphics.Color.parseColor(acc.colorHex)) } catch (_: Exception) { PrimaryMint }
                            PickerItem(
                                id = acc.id,
                                label = acc.name,
                                icon = Icons.Default.Savings,
                                color = color
                            )
                        },
                        onBack = { currentView = SheetView.MAIN },
                        onItemSelected = { accId ->
                            if (accId !in selectedSavingsAccountIds) {
                                selectedSavingsAccountIds.add(accId)
                            }
                            currentView = SheetView.MAIN
                        }
                    )
                }
            }
        }
    }
}

/**
 * Card "+" avente la stessa forma e dimensioni di una card categoria di budget.
 */
@Composable
fun BudgetAddPlaceholderCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = SurfaceElevated.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Aggiungi",
                tint = PrimaryMint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

data class PickerItem(
    val id: Long,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun CategoryGridPickerView(
    title: String,
    items: List<PickerItem>,
    onBack: () -> Unit,
    onItemSelected: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Torna indietro",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tutte le categorie sono già state aggiunte.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    CircularGridItem(
                        icon = item.icon,
                        label = item.label,
                        tintColor = item.color,
                        onClick = { onItemSelected(item.id) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CircularGridItem(
    icon: ImageVector,
    label: String,
    tintColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(tintColor.copy(alpha = 0.20f))
                .border(1.dp, tintColor.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SegmentedModeToggle(
    isPercentage: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (!isPercentage) PrimaryMint else Color.Transparent)
                .clickable { onToggle(false) }
                .padding(horizontal = 9.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "€",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isPercentage) Color.Black else TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isPercentage) PrimaryMint else Color.Transparent)
                .clickable { onToggle(true) }
                .padding(horizontal = 9.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPercentage) Color.Black else TextSecondary
            )
        }
    }
}

@Composable
fun BudgetNumericInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    suffix: String? = null,
    placeholder: String = "0"
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (prefix != null) {
                Text(
                    text = prefix,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryMint
                )
            }

            BasicTextField(
                value = value,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
                    onValueChange(filtered)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                ),
                cursorBrush = SolidColor(PrimaryMint),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 14.sp,
                                color = TextSecondary.copy(alpha = 0.5f),
                                textAlign = TextAlign.End
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (suffix != null) {
                Text(
                    text = suffix,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryMint
                )
            }
        }
    }
}
