package com.edoardo.expensetracker.ui.screens.budgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.BudgetWithProgress
import com.edoardo.expensetracker.data.local.entity.CategoryEntity
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val totalBudget = state.budgets.sumOf { it.amountLimit }
    val remainingBudget = (totalBudget - state.totalSpent).coerceAtLeast(0.0)

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
            com.edoardo.expensetracker.ui.components.FinTechHeaderCard(
                badgeIcon = Icons.Default.PieChart,
                badgeTitle = "BUDGET",
                trailingTopContent = {
                    // Tasto '+' per nuovo budget in squircle elevato
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(11.dp))
                            .clickable { viewModel.openSetBudgetDialog() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aggiungi o Modifica Budget",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                middleContent = {
                    // Numero del Budget Allocato
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
                    // Le 2 chips di riepilogo: Spese Mese e Rimanenti
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

        // Intestazione Sezione Categorie con Barra di Riempimento
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
                    text = "${state.availableCategories.size} categorie",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Lista di TUTTE le categorie con barra di riempimento rispetto al budget
        if (state.availableCategories.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nessuna categoria trovata",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        } else {
            items(state.availableCategories) { catWithSubs ->
                val category = catWithSubs.category
                val budget = state.budgets.find { it.categoryId == category.id }
                val spent = state.categoryStats.find { it.categoryId == category.id }?.totalAmount ?: 0.0

                CategoryBudgetProgressRow(
                    category = category,
                    budget = budget,
                    spent = spent,
                    onClick = {
                        viewModel.openSetBudgetDialog(category.id, budget?.amountLimit ?: 0.0)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Dialog Impostazione Budget
    if (state.isSetBudgetDialogOpen) {
        val selectedCatId = state.selectedCategoryId ?: state.availableCategories.firstOrNull()?.category?.id
        AlertDialog(
            onDismissRequest = { viewModel.closeSetBudgetDialog() },
            containerColor = SurfaceElevated,
            title = { Text("Imposta Budget Categoria", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Seleziona la categoria e il tetto massimo mensile:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // Selettore Categoria a chip orizzontali scorrevoli
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.availableCategories.forEach { catWithSubs ->
                            val cat = catWithSubs.category
                            val isSelected = cat.id == selectedCatId
                            val chipCorner = RoundedCornerShape(12.dp)
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (_: Exception) {
                                PrimaryMint
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(chipCorner)
                                    .clickable { viewModel.onCategorySelectedForBudget(cat.id) },
                                shape = chipCorner,
                                color = if (isSelected) PrimaryMint.copy(alpha = 0.22f) else SurfaceDark,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryMint else SurfaceBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryMint else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.budgetLimitInput,
                        onValueChange = { viewModel.onBudgetLimitInputChange(it) },
                        label = { Text("Limite mensile (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = PrimaryMint,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveBudget() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = Color.Black)
                ) {
                    Text("Salva", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeSetBudgetDialog() }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun CategoryBudgetProgressRow(
    category: CategoryEntity,
    budget: BudgetWithProgress?,
    spent: Double,
    onClick: () -> Unit
) {
    val catColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (_: Exception) {
        PrimaryMint
    }

    val hasBudget = budget != null && budget.amountLimit > 0
    val limit = budget?.amountLimit ?: 0.0
    val progressValue = if (hasBudget) {
        (spent / limit).toFloat().coerceIn(0f, 1f)
    } else 0f

    val isOverBudget = hasBudget && spent > limit
    val progressColor = when {
        isOverBudget -> ExpenseRed
        hasBudget && progressValue > 0.85f -> Color(0xFFF59E0B)
        else -> catColor
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(catColor.copy(alpha = 0.18f))
                            .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIconByName(category.name),
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = category.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (hasBudget) {
                                "€ ${String.format(Locale.ITALY, "%,.2f", spent)} di € ${String.format(Locale.ITALY, "%,.2f", limit)}"
                            } else {
                                "Speso: € ${String.format(Locale.ITALY, "%,.2f", spent)} • Nessun budget"
                            },
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (hasBudget) {
                    if (isOverBudget) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        }
                    } else {
                        Text(
                            text = "${String.format(Locale.ITALY, "%.0f", (spent / limit) * 100)}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }
                } else {
                    Text(
                        text = "Imposta +",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryMint
                    )
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
        }
    }
}
