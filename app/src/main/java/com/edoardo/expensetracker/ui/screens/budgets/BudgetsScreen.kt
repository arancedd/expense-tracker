package com.edoardo.expensetracker.ui.screens.budgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.BudgetWithProgress
import com.edoardo.expensetracker.data.local.dao.SavingsBudgetWithProgress
import com.edoardo.expensetracker.data.local.dao.SubcategoryBudgetWithProgress
import com.edoardo.expensetracker.ui.components.FinTechHeaderCard
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel
) {
    val state by viewModel.uiState.collectAsState()

    val totalBudget = state.budgets.sumOf { it.amountLimit } + state.savingsBudgets.sumOf { it.amountLimit }
    val totalEffectiveSpent = state.totalSpent + state.savingsBudgets.sumOf { it.currentSaved }
    val remainingBudget = (totalBudget - totalEffectiveSpent).coerceAtLeast(0.0)
    val hasBudget = totalBudget > 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // --- FinTech Luxury Top Card per Budget ---
            FinTechHeaderCard(
                badgeIcon = Icons.Default.PieChart,
                badgeTitle = "BUDGET",
                trailingTopContent = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(11.dp))
                            .clickable { viewModel.openBudgetSheet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasBudget) Icons.Default.Settings else Icons.Default.Add,
                            contentDescription = if (hasBudget) "Modifica Budget" else "Imposta Budget",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                middleContent = {
                    Text(
                        text = if (totalBudget > 0) {
                            "€ ${String.format(Locale.ITALY, "%,.2f", totalBudget)}"
                        } else {
                            "Nessun budget allocato"
                        },
                        fontSize = if (totalBudget > 0) 28.sp else 20.sp,
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
                        // Chip 1: Spese Mese
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed.copy(alpha = 0.20f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Spese Mese",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "€ ${String.format(Locale.ITALY, "%,.2f", state.totalSpent)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // Chip 2: Rimanenti
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryMint.copy(alpha = 0.20f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = PrimaryMint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Rimanenti",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "€ ${String.format(Locale.ITALY, "%,.2f", remainingBudget)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryMint
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        // Intestazione Sezione Categorie Spese
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Avanzamento Categorie",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "${state.budgets.size} ${if (state.budgets.size == 1) "categoria" else "categorie"}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Se nessun budget è attivo, mostra la card di stato vuoto
        if (state.budgets.isEmpty() && state.savingsBudgets.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(PrimaryMint.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = PrimaryMint,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Nessun budget impostato",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Pianifica le tue entrate previste e assegna un tetto di spesa mensile (fisso o percentuale) alle categorie o risparmi.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { viewModel.openBudgetSheet() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryMint,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imposta Budget", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            // Elenco Categorie con Budget
            items(state.budgets) { budget ->
                val subBudgets = state.subcategoryBudgets.filter { it.categoryId == budget.categoryId }
                CategoryBudgetProgressRow(
                    budget = budget,
                    subBudgets = subBudgets,
                    onClick = { viewModel.openBudgetSheet() }
                )
            }
        }

        // --- Sezione Piani di Risparmio ---
        if (state.savingsBudgets.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Piani di Risparmio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "${state.savingsBudgets.size} ${if (state.savingsBudgets.size == 1) "conto" else "conti"}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            items(state.savingsBudgets) { sb ->
                SavingsBudgetProgressRow(
                    savingsBudget = sb,
                    onClick = { viewModel.openBudgetSheet() }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Bottom Sheet Unificato per Impostazione/Modifica Budget (OneMoney / Ivy Style)
    if (state.isBudgetSheetOpen) {
        EditBudgetSheet(
            incomeCategories = state.incomeCategories,
            expenseCategories = state.expenseCategories,
            savingsAccounts = state.savingsAccounts,
            initialExpectedIncomes = state.savedExpectedIncomes,
            initialBudgets = state.savedBudgets,
            initialSubcategoryBudgets = state.savedSubcategoryBudgets,
            initialSavingsBudgets = state.savedSavingsBudgets,
            onDismiss = { viewModel.closeBudgetSheet() },
            onSave = { incomes, budgets, subBudgets, savingsBudgets ->
                viewModel.saveBudgetConfiguration(incomes, budgets, subBudgets, savingsBudgets)
            }
        )
    }
}

@Composable
fun CategoryBudgetProgressRow(
    budget: BudgetWithProgress,
    subBudgets: List<SubcategoryBudgetWithProgress>,
    onClick: () -> Unit
) {
    val catColor = try {
        Color(android.graphics.Color.parseColor(budget.categoryColor))
    } catch (_: Exception) {
        PrimaryMint
    }

    var isSubListExpanded by remember { mutableStateOf(false) }

    val limit = budget.amountLimit
    val spent = budget.currentSpent
    val progressValue = if (limit > 0) {
        (spent / limit).toFloat().coerceIn(0f, 1f)
    } else 0f

    val isOverBudget = spent > limit
    val progressColor = when {
        isOverBudget -> ExpenseRed
        progressValue > 0.85f -> Color(0xFFF59E0B)
        else -> catColor
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                if (budget.hasSubcategoriesBudget && subBudgets.isNotEmpty()) {
                    isSubListExpanded = !isSubListExpanded
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            imageVector = getCategoryIconByName(budget.categoryName),
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = budget.categoryName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            if (budget.isPercentage) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(catColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.ITALY, "%.0f", budget.percentageValue)}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = catColor
                                    )
                                }
                            }
                            if (budget.hasSubcategoriesBudget) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryMint.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${subBudgets.size} sub",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryMint
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "€ ${String.format(Locale.ITALY, "%,.2f", spent)} di € ${String.format(Locale.ITALY, "%,.2f", limit)}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverBudget) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Sforato",
                            tint = ExpenseRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sforato",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    } else {
                        Text(
                            text = "${String.format(Locale.ITALY, "%.0f", budget.percentageUsed)}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }

                    if (budget.hasSubcategoriesBudget && subBudgets.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isSubListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra di Riempimento rispetto al budget
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = SurfaceDark
            )

            // Dettaglio Sottocategorie espandibile
            AnimatedVisibility(visible = isSubListExpanded && subBudgets.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)
                    subBudgets.forEach { sub ->
                        val subProgress = if (sub.amountLimit > 0) (sub.currentSpent / sub.amountLimit).toFloat().coerceIn(0f, 1f) else 0f
                        val subOver = sub.currentSpent > sub.amountLimit

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sub.subcategoryName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "€ ${String.format(Locale.ITALY, "%,.2f", sub.currentSpent)} / € ${String.format(Locale.ITALY, "%,.2f", sub.amountLimit)}",
                                fontSize = 11.sp,
                                color = if (subOver) ExpenseRed else TextSecondary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { subProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (subOver) ExpenseRed else catColor.copy(alpha = 0.8f),
                            trackColor = SurfaceDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsBudgetProgressRow(
    savingsBudget: SavingsBudgetWithProgress,
    onClick: () -> Unit
) {
    val accColor = try {
        Color(android.graphics.Color.parseColor(savingsBudget.accountColor))
    } catch (_: Exception) {
        PrimaryMint
    }

    val limit = savingsBudget.amountLimit
    val saved = savingsBudget.currentSaved
    val progressValue = if (limit > 0) {
        (saved / limit).toFloat().coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = savingsBudget.accountName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            if (savingsBudget.isPercentage) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryMint.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.ITALY, "%.0f", savingsBudget.percentageValue)}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryMint
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "€ ${String.format(Locale.ITALY, "%,.2f", saved)} di € ${String.format(Locale.ITALY, "%,.2f", limit)}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (savingsBudget.isGoalReached) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Raggiunto",
                            tint = PrimaryMint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Raggiunto",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryMint
                        )
                    }
                } else {
                    Text(
                        text = "${String.format(Locale.ITALY, "%.0f", savingsBudget.percentageUsed)}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryMint
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryMint,
                trackColor = SurfaceDark
            )
        }
    }
}
