package com.edoardo.expensetracker.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.CategoryWithSubcategories
import com.edoardo.expensetracker.data.local.entity.SubcategoryEntity
import com.edoardo.expensetracker.ui.theme.ExpenseRed
import com.edoardo.expensetracker.ui.theme.PrimaryMint
import com.edoardo.expensetracker.ui.theme.SurfaceBorder
import com.edoardo.expensetracker.ui.theme.SurfaceDark
import com.edoardo.expensetracker.ui.theme.SurfaceElevated
import com.edoardo.expensetracker.ui.theme.TextPrimary
import com.edoardo.expensetracker.ui.theme.TextSecondary

import com.edoardo.expensetracker.ui.components.CATEGORY_ICONS_LIST

val PRESET_COLORS = listOf(
    "#EF4444", "#F59E0B", "#10B981", "#06B6D4", "#3B82F6",
    "#6366F1", "#8B5CF6", "#EC4899", "#14B8A6", "#84CC16"
)

val AVAILABLE_ICONS = CATEGORY_ICONS_LIST

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryBottomSheet(
    categoryWithSubs: CategoryWithSubcategories,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconName: String, isArchived: Boolean) -> Unit,
    onAddSubcategory: (name: String, iconName: String) -> Unit,
    onToggleSubcategoryArchived: (SubcategoryEntity) -> Unit,
    onDeleteSubcategory: (SubcategoryEntity) -> Unit
) {
    val category = categoryWithSubs.category

    var name by remember { mutableStateOf(category.name) }
    var selectedColor by remember { mutableStateOf(category.colorHex) }
    var selectedIcon by remember { mutableStateOf(category.iconName) }
    var isArchived by remember { mutableStateOf(category.isArchived) }

    var newSubcategoryName by remember { mutableStateOf("") }
    val newSubcategoryIcon = "label"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Modifica Categoria", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Button(
                        onClick = {
                            onSave(name, selectedColor, selectedIcon, isArchived)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salva", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Nome Categoria
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Categoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedBorderColor = PrimaryMint,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Selettore Colore
            item {
                Text("Colore Categoria:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(PRESET_COLORS) { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, TextPrimary, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }
            }

            // Selettore Icona
            item {
                Text("Icona Categoria:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(AVAILABLE_ICONS) { pair ->
                        val isSelected = selectedIcon == pair.first
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryMint.copy(alpha = 0.2f) else SurfaceElevated)
                                .border(1.dp, if (isSelected) PrimaryMint else SurfaceBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedIcon = pair.first },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                pair.second,
                                contentDescription = pair.first,
                                tint = if (isSelected) PrimaryMint else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Switch Archiviazione
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Archivia Categoria", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                        Text("Nascondi la categoria dalla dashboard principale", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isArchived,
                        onCheckedChange = { isArchived = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryMint,
                            uncheckedTrackColor = SurfaceBorder
                        )
                    )
                }
            }

            item {
                HorizontalDivider(color = SurfaceBorder)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Sottocategorie:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }

            // Lista Sottocategorie
            items(categoryWithSubs.subcategories) { sub ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.name,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (sub.isArchived) TextSecondary.copy(alpha = 0.5f) else TextPrimary
                            )
                            if (sub.isArchived) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(Archiviata)", fontSize = 11.sp, color = ExpenseRed)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onToggleSubcategoryArchived(sub) }) {
                                Icon(
                                    if (sub.isArchived) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Archivia",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteSubcategory(sub) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Aggiungi Nuova Sottocategoria
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubcategoryName,
                        onValueChange = { newSubcategoryName = it },
                        placeholder = { Text("Nuova sottocategoria...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceElevated,
                            unfocusedContainerColor = SurfaceElevated,
                            focusedBorderColor = PrimaryMint,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    IconButton(
                        onClick = {
                            if (newSubcategoryName.isNotBlank()) {
                                onAddSubcategory(newSubcategoryName, newSubcategoryIcon)
                                newSubcategoryName = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryMint, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi")
                    }
                }
            }
        }
    }
}
