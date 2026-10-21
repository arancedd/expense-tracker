package com.edoardo.expensetracker.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.entity.AccountEntity
import com.edoardo.expensetracker.data.local.entity.AccountType
import com.edoardo.expensetracker.ui.components.getAccountTypeLabel
import com.edoardo.expensetracker.ui.screens.categories.PRESET_COLORS
import com.edoardo.expensetracker.ui.theme.*

@Composable
fun EditAccountDialog(
    account: AccountEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, colorHex: String, isActive: Boolean, includeInTotalBalance: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var selectedType by remember { mutableStateOf(account.accountType) }
    var selectedColor by remember { mutableStateOf(account.colorHex) }
    var isActive by remember { mutableStateOf(account.isActive) }
    var includeInTotalBalance by remember { mutableStateOf(account.includeInTotalBalance) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Modifica Conto", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Conto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryMint,
                        unfocusedBorderColor = SurfaceBorder
                    )
                )

                Text("Tipologia:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(getAccountTypeLabel(type), fontSize = 11.sp) }
                        )
                    }
                }

                Text("Colore Conto:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PRESET_COLORS) { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex }
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, TextPrimary, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }

                // Switch Includi nel Total Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Includi nel Saldo Totale", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
                        Text("Mostra questo conto nel patrimonio totale netto", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = includeInTotalBalance,
                        onCheckedChange = { includeInTotalBalance = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryMint,
                            uncheckedTrackColor = SurfaceBorder
                        )
                    )
                }

                // Switch Archiviazione Conto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Conto Attivo", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
                        Text("Disattiva per archiviare il conto senza cancellarlo", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryMint,
                            uncheckedTrackColor = SurfaceBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, selectedType, selectedColor, isActive, includeInTotalBalance)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = Color.Black)
            ) {
                Text("Salva", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondary)
            }
        }
    )
}
