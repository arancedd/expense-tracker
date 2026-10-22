package com.edoardo.expensetracker.ui.screens.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.edoardo.expensetracker.data.local.entity.AccountEntity
import com.edoardo.expensetracker.data.local.entity.AccountType
import com.edoardo.expensetracker.ui.components.IconAndColorPickerDialog
import com.edoardo.expensetracker.ui.components.getAccountIcon
import com.edoardo.expensetracker.ui.theme.*

/**
 * Bottom Sheet per Modifica e Creazione Conto (OneMoney / Ivy Style).
 * - Nome conto a sinistra + Squircle Icona & Colore a destra (apre dialog con schede Icone e Colori)
 * - Selezione dei 3 tipi di conto: Regolare, Risparmi, Debiti
 * - Valuta ("Euro — €") e Descrizione / Note
 * - Switch "Includi nel totale" e "Archivia conto"
 * - Tasto "Elimina conto"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountSheet(
    account: AccountEntity? = null,
    initialType: AccountType = AccountType.REGULAR,
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, colorHex: String, iconName: String, isActive: Boolean, includeInTotal: Boolean, initialBalance: Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var initialBalanceStr by remember { mutableStateOf(if (account == null) "" else String.format(java.util.Locale.US, "%.2f", account.initialBalance)) }
    var selectedType by remember {
        mutableStateOf(
            account?.accountType ?: initialType
        )
    }
    var selectedColor by remember {
        mutableStateOf(
            account?.colorHex ?: if (initialType == AccountType.INVESTMENT) "#0EA5E9" else "#10B981"
        )
    }
    var selectedIcon by remember {
        mutableStateOf(
            account?.iconName ?: if (initialType == AccountType.INVESTMENT) "trending_up" else "account_balance"
        )
    }
    var isActive by remember { mutableStateOf(account?.isActive ?: true) }
    var includeInTotal by remember { mutableStateOf(account?.includeInTotalBalance ?: true) }
    var description by remember { mutableStateOf("") }

    var showIconColorPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val accColor = try {
        Color(android.graphics.Color.parseColor(selectedColor))
    } catch (_: Exception) {
        PrimaryMint
    }

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
                        text = if (account != null) "Modifica Conto" else "Nuovo Conto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val initBal = initialBalanceStr.toDoubleOrNull() ?: 0.0
                            onSave(name.trim(), selectedType, selectedColor, selectedIcon, isActive, includeInTotal, initBal)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accColor, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Salva", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Nome + Squircle Icona & Colore
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
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nome Conto",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                BasicTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    cursorBrush = SolidColor(accColor),
                                    decorationBox = { innerTextField ->
                                        if (name.isEmpty()) {
                                            Text("es. Conto Principale...", fontSize = 20.sp, color = TextMuted)
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Squircle Icona che apre il picker a 2 schede
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(accColor)
                                    .clickable { showIconColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getAccountIcon(selectedType),
                                    contentDescription = "Cambia icona e colore",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Tipologia di Conto (Regolare, Risparmi, Debiti, Investimenti)
                item {
                    Text(
                        text = "Tipo di Conto",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryMint,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(AccountType.REGULAR, "Regolare", Icons.Default.AccountBalance),
                            Triple(AccountType.SAVINGS, "Risparmi", Icons.Default.Savings),
                            Triple(AccountType.DEBT, "Debiti", Icons.Default.CreditCard),
                            Triple(AccountType.INVESTMENT, "Investimenti", Icons.AutoMirrored.Filled.TrendingUp)
                        ).forEach { (type, label, icon) ->
                            val isSelected = selectedType == type
                            val typeColor = when (type) {
                                AccountType.DEBT -> ExpenseRed
                                AccountType.SAVINGS -> SecondaryViolet
                                AccountType.INVESTMENT -> Color(0xFF0EA5E9)
                                else -> PrimaryMint
                            }
                            val corner = RoundedCornerShape(12.dp)

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(corner)
                                    .clickable { selectedType = type },
                                shape = corner,
                                color = if (isSelected) typeColor.copy(alpha = 0.20f) else SurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) typeColor else SurfaceBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) typeColor else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) typeColor else TextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Saldo Iniziale (Solo per nuovo conto, non visibile per Investimenti)
                if (account == null) {
                    item {
                        if (selectedType == AccountType.INVESTMENT) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF0EA5E9).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Il saldo si aggiornerà automaticamente con il controvalore dei tuoi stock comprati o venduti. Non richiede saldo iniziale.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, SurfaceBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Saldo Iniziale (€)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    BasicTextField(
                                        value = initialBalanceStr,
                                        onValueChange = { initialBalanceStr = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (initialBalanceStr.isEmpty()) {
                                                Text(
                                                    "0.00",
                                                    color = TextMuted,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Sezione Impostazioni: Valuta e Descrizione
                item {
                    Text(
                        text = "Impostazioni",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryMint,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Euro, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Valuta del conto", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Euro — €", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Descrizione", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    BasicTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = TextSecondary, fontSize = 12.sp),
                                        decorationBox = { innerTextField ->
                                            if (description.isEmpty()) {
                                                Text("Aggiungi una descrizione o nota...", fontSize = 12.sp, color = TextMuted)
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Switch Includi nel Totale Patrimoniale
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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Includi nel Saldo Totale", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Mostra questo saldo nel patrimonio complessivo", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = includeInTotal,
                                onCheckedChange = { includeInTotal = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryMint,
                                    checkedTrackColor = PrimaryMint.copy(alpha = 0.35f)
                                )
                            )
                        }
                    }
                }

                // Switch Archivia Conto
                if (account != null) {
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
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Archivia conto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                                Switch(
                                    checked = !isActive,
                                    onCheckedChange = { isActive = !it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PrimaryMint,
                                        checkedTrackColor = PrimaryMint.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        }
                    }

                    // Tasto Elimina Conto
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showDeleteConfirmDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            color = ExpenseRed.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Elimina conto", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showIconColorPicker) {
        IconAndColorPickerDialog(
            initialIcon = selectedIcon,
            initialColorHex = selectedColor,
            onDismiss = { showIconColorPicker = false },
            onConfirm = { icon, color ->
                selectedIcon = icon
                selectedColor = color
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Eliminare il conto?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Questa azione eliminerà il conto e tutte le transazioni collegate.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete?.invoke()
                        onDismiss()
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
