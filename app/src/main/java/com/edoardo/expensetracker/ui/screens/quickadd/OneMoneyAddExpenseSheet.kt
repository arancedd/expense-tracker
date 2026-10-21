package com.edoardo.expensetracker.ui.screens.quickadd

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.components.CalendarPeriodDialog
import com.edoardo.expensetracker.ui.components.OneMoneyCalculatorKeypad
import com.edoardo.expensetracker.ui.components.getAccountIcon
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class QuickAddSheetMode {
    FORM,
    SELECT_SOURCE_ACCOUNT,
    SELECT_DEST_ACCOUNT,
    SELECT_SOURCE_CATEGORY_INCOME,
    SELECT_DEST_CATEGORY_EXPENSE
}

/**
 * Bottom Sheet per Creazione e Modifica Transazioni (OneMoney / Ivy Wallet style).
 * - Nessuna freccia centrale tra le due card (schede affiancate armoniose)
 * - Nessun accavallamento di bottom sheets: le selezioni avvengono tramite transizione interna fluida nello stesso sheet
 * - Logica contestuale 100% pulita:
 *     * Spesa: Sinistra = Conto (apre solo griglia conti), Destra = Categoria Spesa (apre solo griglia categorie spesa)
 *     * Entrata: Sinistra = Categoria Entrata (apre solo griglia categorie entrate), Destra = Conto (apre solo griglia conti)
 *     * Giroconto: Sinistra = Conto di partenza, Destra = Conto di arrivo (apre solo griglia conti)
 * - In modalità dettaglio/modifica (da click transazione):
 *     * Layout fedele allo screenshot: 3 pulsanti tondi inferiori (Elimina, Data, Duplica)
 *     * Cliccando sull'importo si apre il keypad di calcolo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneMoneyAddExpenseSheet(
    viewModel: QuickAddViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var sheetMode by remember { mutableStateOf(QuickAddSheetMode.FORM) }
    var slideDirection by remember { mutableStateOf(1) } // -1 = chip sinistra (da sinistra), 1 = chip destra (da destra)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val isEditing = state.editingTransactionId != null
    var showKeypadInEdit by remember(state.editingTransactionId) { mutableStateOf(!isEditing) }

    val isTransfer = state.type == TransactionType.TRANSFER
    val isIncome = state.type == TransactionType.INCOME
    val isExpense = state.type == TransactionType.EXPENSE

    val currentAccount = state.accounts.firstOrNull { it.account.id == state.selectedAccountId }
        ?: state.accounts.firstOrNull()

    val currentDestAccount = state.accounts.firstOrNull { it.account.id == state.selectedDestinationAccountId }
        ?: state.accounts.firstOrNull { it.account.id != currentAccount?.account?.id }

    val availableCategories = if (isIncome) {
        state.incomeCategories
    } else {
        state.expenseCategories
    }
    val currentCategory = availableCategories.firstOrNull { it.category.id == state.selectedCategoryId }
        ?: availableCategories.firstOrNull()

    val categoryColor = currentCategory?.category?.colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { PrimaryMint }
    } ?: PrimaryMint

    val accountColor = currentAccount?.account?.colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { SecondaryViolet }
    } ?: SecondaryViolet

    val destAccountColor = currentDestAccount?.account?.colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { PrimaryMint }
    } ?: PrimaryMint

    val activeConfirmColor = when {
        isTransfer -> SecondaryViolet
        isIncome -> PrimaryMint
        else -> categoryColor
    }

    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ITALY)
    val txDateStr = dateFormat.format(Date(state.dateMillis)).replaceFirstChar { it.uppercase() }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetState()
            onDismiss()
        },
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp)
        ) {
            AnimatedContent(
                targetState = sheetMode,
                transitionSpec = {
                    if (targetState != QuickAddSheetMode.FORM) {
                        // Andata verso il selettore: segue la direzione della chip cliccata
                        if (slideDirection == -1) {
                            (slideInHorizontally(tween(260)) { -it } + fadeIn(tween(240)))
                                .togetherWith(slideOutHorizontally(tween(220)) { it / 2 } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally(tween(260)) { it } + fadeIn(tween(240)))
                                .togetherWith(slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(200)))
                        }
                    } else {
                        // Ritorno alla calcolatrice: direzione esattamente opposta a quella di andata
                        if (slideDirection == -1) {
                            (slideInHorizontally(tween(240)) { it / 2 } + fadeIn(tween(240)))
                                .togetherWith(slideOutHorizontally(tween(260)) { -it } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally(tween(240)) { -it / 2 } + fadeIn(tween(240)))
                                .togetherWith(slideOutHorizontally(tween(260)) { it } + fadeOut(tween(200)))
                        }
                    }
                },
                label = "quick_add_sheet_view"
            ) { mode ->
                when (mode) {
                    QuickAddSheetMode.FORM -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        // --- Schede Affiancate Superiori (SENZA freccia centrale) ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val cardCorner = RoundedCornerShape(18.dp)

                            // Scheda Sinistra:
                            // In Income -> DA CATEGORIA
                            // In Expense o Transfer -> DA CONTO
                            val leftTitle = if (isIncome) "DA CATEGORIA" else "DA CONTO"
                            val leftName = if (isIncome) {
                                currentCategory?.category?.name ?: "Categoria"
                            } else {
                                currentAccount?.account?.name ?: "Conto"
                            }
                            val leftColor = if (isIncome) categoryColor else accountColor
                            val leftIcon = if (isIncome) {
                                currentCategory?.category?.iconName?.let { getCategoryIconByName(it) } ?: Icons.AutoMirrored.Filled.Label
                            } else {
                                currentAccount?.account?.accountType?.let { getAccountIcon(it) } ?: Icons.Default.CreditCard
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(82.dp)
                                    .clip(cardCorner)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                leftColor.copy(alpha = 0.22f),
                                                SurfaceElevated
                                            )
                                        )
                                    )
                                    .border(1.dp, leftColor.copy(alpha = 0.40f), cardCorner)
                                    .clickable {
                                        slideDirection = -1
                                        sheetMode = if (isIncome) {
                                            QuickAddSheetMode.SELECT_SOURCE_CATEGORY_INCOME
                                        } else {
                                            QuickAddSheetMode.SELECT_SOURCE_ACCOUNT
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = leftTitle,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = leftName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(leftColor.copy(alpha = 0.20f))
                                        .border(1.dp, leftColor.copy(alpha = 0.45f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = leftIcon,
                                        contentDescription = null,
                                        tint = leftColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Scheda Destra:
                            // In Transfer -> A CONTO
                            // In Income -> A CONTO
                            // In Expense -> A CATEGORIA
                            val rightTitle = when {
                                isTransfer -> "A CONTO"
                                isIncome -> "A CONTO"
                                else -> "A CATEGORIA"
                            }
                            val rightName = when {
                                isTransfer -> currentDestAccount?.account?.name ?: "Conto di arrivo"
                                isIncome -> currentAccount?.account?.name ?: "Conto accredito"
                                else -> currentCategory?.category?.name ?: "Categoria"
                            }
                            val rightColor = when {
                                isTransfer -> destAccountColor
                                isIncome -> accountColor
                                else -> categoryColor
                            }
                            val rightIcon = when {
                                isTransfer -> currentDestAccount?.account?.accountType?.let { getAccountIcon(it) } ?: Icons.Default.SwapHoriz
                                isIncome -> currentAccount?.account?.accountType?.let { getAccountIcon(it) } ?: Icons.Default.AccountBalance
                                else -> currentCategory?.category?.iconName?.let { getCategoryIconByName(it) } ?: Icons.AutoMirrored.Filled.Label
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(82.dp)
                                    .clip(cardCorner)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                rightColor.copy(alpha = 0.22f),
                                                SurfaceElevated
                                            )
                                        )
                                    )
                                    .border(1.dp, rightColor.copy(alpha = 0.40f), cardCorner)
                                    .clickable {
                                        slideDirection = 1
                                        sheetMode = when {
                                            isTransfer -> QuickAddSheetMode.SELECT_DEST_ACCOUNT
                                            isIncome -> QuickAddSheetMode.SELECT_DEST_ACCOUNT
                                            else -> QuickAddSheetMode.SELECT_DEST_CATEGORY_EXPENSE
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = rightTitle,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = rightName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(rightColor.copy(alpha = 0.20f))
                                        .border(1.dp, rightColor.copy(alpha = 0.45f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = rightIcon,
                                        contentDescription = null,
                                        tint = rightColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // --- Subcategories Chips (se non è giroconto) ---
                        if (!isTransfer) {
                            val subcategories = currentCategory?.subcategories ?: emptyList()
                            if (subcategories.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    subcategories.forEach { sub ->
                                        val isSelected = state.selectedSubcategoryId == sub.id
                                        val chipCorner = RoundedCornerShape(12.dp)
                                        Surface(
                                            modifier = Modifier
                                                .clip(chipCorner)
                                                .clickable {
                                                    viewModel.onSubcategorySelected(
                                                        if (isSelected) null else sub.id
                                                    )
                                                },
                                            shape = chipCorner,
                                            color = if (isSelected) categoryColor.copy(alpha = 0.22f) else SurfaceElevated,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) categoryColor else SurfaceBorder
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIconByName(sub.iconName),
                                                    contentDescription = null,
                                                    tint = if (isSelected) categoryColor else TextSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = sub.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) categoryColor else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        // --- Area Importo e Tipo ---
                        val typeLabel = when {
                            isTransfer -> "Giroconto"
                            isIncome -> "Entrata"
                            else -> "Spesa"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clickable {
                                    if (isEditing) {
                                        showKeypadInEdit = !showKeypadInEdit
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val displayAmountText = if (state.amountExpression.isEmpty()) "0" else state.amountExpression
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "€",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Light,
                                    color = activeConfirmColor.copy(alpha = 0.70f),
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = displayAmountText,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = activeConfirmColor,
                                    maxLines = 1
                                )
                            }

                            if (isEditing && !showKeypadInEdit) {
                                Text(
                                    text = "Tocca per modificare l'importo",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Campo Errori
                        state.errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = ExpenseRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // In modalità Modifica con Keypad chiuso: mostra Notes Box, Data e i 3 Pulsanti Azione
                        // Cliccando sull'importo, il keypad scorre verso l'alto con animazione fluida verticale
                        AnimatedContent(
                            targetState = (isEditing && !showKeypadInEdit),
                            transitionSpec = {
                                if (!targetState) {
                                    (slideInVertically(tween(320)) { it } + fadeIn(tween(250)))
                                        .togetherWith(slideOutVertically(tween(250)) { -it / 3 } + fadeOut(tween(200)))
                                } else {
                                    (slideInVertically(tween(250)) { -it / 3 } + fadeIn(tween(250)))
                                        .togetherWith(slideOutVertically(tween(320)) { it } + fadeOut(tween(200)))
                                }
                            },
                            label = "edit_keypad_slide_transition"
                        ) { isShowingDetailView ->
                            if (isShowingDetailView) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Box Note
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = SurfaceElevated,
                                        border = BorderStroke(1.dp, SurfaceBorder)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            if (state.note.isEmpty()) {
                                                Text(
                                                    text = "Note...",
                                                    fontSize = 13.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            BasicTextField(
                                                value = state.note,
                                                onValueChange = { viewModel.onNoteChange(it) },
                                                singleLine = false,
                                                maxLines = 3,
                                                textStyle = TextStyle(
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                cursorBrush = SolidColor(activeConfirmColor),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Data della transazione
                                    Text(
                                        text = txDateStr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // I 3 Grandi Bottoni Rotondi (Elimina, Data, Duplica - Screenshot utente)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RoundTransactionButton(
                                            icon = Icons.Default.DeleteOutline,
                                            label = "Elimina",
                                            iconColor = ExpenseRed,
                                            onClick = { showDeleteConfirmDialog = true }
                                        )

                                        RoundTransactionButton(
                                            icon = Icons.Default.CalendarToday,
                                            label = "Data",
                                            iconColor = SecondaryViolet,
                                            onClick = { showDatePickerDialog = true }
                                        )

                                        RoundTransactionButton(
                                            icon = Icons.Default.ContentCopy,
                                            label = "Duplica",
                                            iconColor = PrimaryMint,
                                            onClick = {
                                                viewModel.duplicateCurrentTransaction(onSuccess = {
                                                    onDismiss()
                                                })
                                            }
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Modalità Creazione (o Modifica con keypad attivo): Campo note + Keypad calcolatrice
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .padding(bottom = 6.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = SurfaceElevated.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, SurfaceBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EditNote,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            BasicTextField(
                                                value = state.note,
                                                onValueChange = { viewModel.onNoteChange(it) },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    textAlign = TextAlign.Center
                                                ),
                                                cursorBrush = SolidColor(activeConfirmColor),
                                                decorationBox = { innerTextField ->
                                                    if (state.note.isEmpty()) {
                                                        Text(
                                                            "Aggiungi causale o nota...",
                                                            fontSize = 12.sp,
                                                            color = TextMuted,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    innerTextField()
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Keypad calcolatrice moderno
                                    OneMoneyCalculatorKeypad(
                                        currentExpression = state.amountExpression,
                                        onExpressionChange = { viewModel.onAmountExpressionChange(it) },
                                        onConfirm = {
                                            viewModel.saveTransaction(onSuccess = {
                                                viewModel.resetState()
                                                onDismiss()
                                            })
                                        },
                                        onDateClick = { showDatePickerDialog = true },
                                        confirmColor = activeConfirmColor
                                    )

                                    Text(
                                        text = txDateStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                QuickAddSheetMode.SELECT_SOURCE_ACCOUNT,
                QuickAddSheetMode.SELECT_DEST_ACCOUNT -> {
                    // Selezione Conto (Origine o Destinazione)
                    val isSource = mode == QuickAddSheetMode.SELECT_SOURCE_ACCOUNT
                    val title = if (isSource) "Seleziona Conto di Origine" else "Seleziona Conto di Destinazione"
                    val accountsList = if (!isSource && isTransfer) {
                        state.accounts.filter { it.account.id != state.selectedAccountId }
                    } else {
                        state.accounts
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { sheetMode = QuickAddSheetMode.FORM },
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (accountsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Nessun altro conto disponibile.", color = TextSecondary, fontSize = 13.sp)
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
                                items(accountsList) { accWithBal ->
                                    val acc = accWithBal.account
                                    val isSelected = if (isSource) acc.id == state.selectedAccountId else acc.id == state.selectedDestinationAccountId
                                    val color = try {
                                        Color(android.graphics.Color.parseColor(acc.colorHex))
                                    } catch (_: Exception) { SecondaryViolet }

                                    CircularGridItem(
                                        icon = getAccountIcon(acc.accountType),
                                        label = acc.name,
                                        tintColor = color,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSource) {
                                                viewModel.onAccountSelected(acc.id)
                                            } else {
                                                viewModel.onDestinationAccountSelected(acc.id)
                                            }
                                            sheetMode = QuickAddSheetMode.FORM
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                QuickAddSheetMode.SELECT_DEST_CATEGORY_EXPENSE -> {
                    // Selezione Categoria Spesa
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { sheetMode = QuickAddSheetMode.FORM },
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
                                text = "Seleziona Categoria Spesa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.expenseCategories) { catWithSubs ->
                                val cat = catWithSubs.category
                                val isSelected = cat.id == state.selectedCategoryId
                                val color = try {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                } catch (_: Exception) { PrimaryMint }

                                CircularGridItem(
                                    icon = getCategoryIconByName(cat.iconName),
                                    label = cat.name,
                                    tintColor = color,
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.onCategorySelected(cat.id)
                                        sheetMode = QuickAddSheetMode.FORM
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                QuickAddSheetMode.SELECT_SOURCE_CATEGORY_INCOME -> {
                    // Selezione Categoria Entrata
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { sheetMode = QuickAddSheetMode.FORM },
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
                                text = "Seleziona Categoria Entrata",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.incomeCategories) { catWithSubs ->
                                val cat = catWithSubs.category
                                val isSelected = cat.id == state.selectedCategoryId
                                val color = try {
                                    Color(android.graphics.Color.parseColor(cat.colorHex))
                                } catch (_: Exception) { PrimaryMint }

                                CircularGridItem(
                                    icon = getCategoryIconByName(cat.iconName),
                                    label = cat.name,
                                    tintColor = color,
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.onCategorySelected(cat.id)
                                        sheetMode = QuickAddSheetMode.FORM
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
        }
    }

    // Dialog selezione Data
    if (showDatePickerDialog) {
        CalendarPeriodDialog(
            currentPeriod = null,
            currentDateMillis = state.dateMillis,
            selectedRangeStartMillis = null,
            selectedRangeEndMillis = null,
            onSelectPeriod = {},
            onSelectDay = { cal ->
                viewModel.onDateChange(cal.timeInMillis)
                showDatePickerDialog = false
            },
            onSelectRange = { _, _ -> },
            onResetToDefault = {
                viewModel.onDateChange(System.currentTimeMillis())
                showDatePickerDialog = false
            },
            onDismissRequest = { showDatePickerDialog = false },
            defaultActionLabel = "Oggi (Default)"
        )
    }

    // Conferma eliminazione transazione
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Eliminare la transazione?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("La transazione verrà cancellata definitivamente e il saldo dei conti aggiornato.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteCurrentTransaction(onSuccess = {
                            onDismiss()
                        })
                    }
                ) {
                    Text("Elimina", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Singolo elemento circolare per la griglia a 4 colonne.
 */
@Composable
private fun CircularGridItem(
    icon: ImageVector,
    label: String,
    tintColor: Color,
    isSelected: Boolean,
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
                .background(if (isSelected) tintColor else tintColor.copy(alpha = 0.20f))
                .border(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) Color.White else tintColor.copy(alpha = 0.45f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextPrimary else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Bottone rotondo di azione (Elimina, Data, Duplica - screenshot utente).
 */
@Composable
private fun RoundTransactionButton(
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
