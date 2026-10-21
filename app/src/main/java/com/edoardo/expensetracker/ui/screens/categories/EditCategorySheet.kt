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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.CategoryWithSubcategories
import com.edoardo.expensetracker.data.local.entity.SubcategoryEntity
import com.edoardo.expensetracker.ui.components.IconAndColorPickerDialog
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.theme.*

enum class EditCategoryPage {
    CATEGORY,
    SUBCATEGORY
}

/**
 * Bottom Sheet a schermo intero/espandibile per Modifica Categoria (OneMoney / Ivy Style da screenshot utente).
 * - Nome a sinistra + Grande Squircle Icona & Colore a destra (apre dialog con schede Icone e Colori)
 * - Sezione Impostazioni (Valuta)
 * - Sezione Sottocategorie con icone dedicate e aggiunta rapida
 * - Switch Archiviazione Categoria
 * - Tasto Eliminazione Categoria
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategorySheet(
    categoryWithSubs: CategoryWithSubcategories? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconName: String, isArchived: Boolean) -> Unit,
    onAddSubcategory: (name: String, iconName: String) -> Unit,
    onToggleSubcategoryArchived: (SubcategoryEntity) -> Unit,
    onDeleteSubcategory: (SubcategoryEntity) -> Unit,
    onUpdateSubcategory: (SubcategoryEntity) -> Unit = {},
    onDeleteCategory: (() -> Unit)? = null
) {
    val category = categoryWithSubs?.category

    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: "#10B981") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "category") }
    var isArchived by remember { mutableStateOf(category?.isArchived ?: false) }

    var showIconColorPicker by remember { mutableStateOf(false) }
    var showAddSubcategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var page by remember { mutableStateOf(EditCategoryPage.CATEGORY) }
    var targetSubForEdit by remember { mutableStateOf<SubcategoryEntity?>(null) }
    var editSubName by remember { mutableStateOf("") }
    var editSubIcon by remember { mutableStateOf("label") }
    var editSubArchived by remember { mutableStateOf(false) }
    var showEditSubIconPicker by remember { mutableStateOf(false) }

    val catColor = try {
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
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState == EditCategoryPage.CATEGORY) {
                    (slideInHorizontally(tween(260)) { -it / 2 } + fadeIn(tween(240)))
                        .togetherWith(slideOutHorizontally(tween(240)) { it } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(260)) { it } + fadeIn(tween(240)))
                        .togetherWith(slideOutHorizontally(tween(220)) { -it / 2 } + fadeOut(tween(200)))
                }
            },
            label = "edit_category_sheet_pages"
        ) { curPage ->
            when (curPage) {
                EditCategoryPage.CATEGORY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
            // Top Bar: Indietro, Titolo "Categoria", e Salva
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
                        text = if (category != null) "Modifica Categoria" else "Nuova Categoria",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name.trim(), selectedColor, selectedIcon, isArchived)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = catColor, contentColor = Color.White),
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Nome (sinistra) + Squircle Icona/Colore (destra)
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
                                    text = "Nome",
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
                                    cursorBrush = SolidColor(catColor),
                                    decorationBox = { innerTextField ->
                                        if (name.isEmpty()) {
                                            Text("Nome categoria...", fontSize = 20.sp, color = TextMuted)
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Squircle Icona con colore categoria che apre il dialog a 2 schede
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(catColor)
                                    .clickable { showIconColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIconByName(selectedIcon),
                                    contentDescription = "Cambia icona e colore",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Sezione Impostazioni: Valuta
                item {
                    Text(
                        text = "Impostazioni",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = catColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Euro,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Valuta categoria", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Euro — €", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // Sezione Sottocategorie
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sottocategorie",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        TextButton(
                            onClick = { showAddSubcategoryDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = catColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aggiungi", color = catColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val subs = categoryWithSubs?.subcategories ?: emptyList()
                if (subs.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Nessuna sottocategoria presente.", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    items(subs) { sub ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    targetSubForEdit = sub
                                    editSubName = sub.name
                                    editSubIcon = sub.iconName
                                    editSubArchived = sub.isArchived
                                    page = EditCategoryPage.SUBCATEGORY
                                },
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
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(catColor.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIconByName(sub.iconName),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = sub.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (sub.isArchived) TextMuted else TextPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onToggleSubcategoryArchived(sub) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (sub.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = "Archivia",
                                            tint = if (sub.isArchived) catColor else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSubcategory(sub) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Elimina",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (categoryWithSubs != null) {
                    // Switch Archivia Categoria
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
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Archivia categoria",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }

                                Switch(
                                    checked = isArchived,
                                    onCheckedChange = { isArchived = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PrimaryMint,
                                        checkedTrackColor = PrimaryMint.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        }
                    }

                    // Tasto Elimina Categoria (Rosso)
                    if (onDeleteCategory != null) {
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
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Elimina categoria",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
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

    EditCategoryPage.SUBCATEGORY -> {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar: Indietro, Titolo "Modifica Sottocategoria", e Salva
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { page = EditCategoryPage.CATEGORY },
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
                        text = "Modifica Sottocategoria",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = {
                        targetSubForEdit?.let { target ->
                            if (editSubName.isNotBlank()) {
                                onUpdateSubcategory(
                                    target.copy(
                                        name = editSubName.trim(),
                                        iconName = editSubIcon,
                                        isArchived = editSubArchived
                                    )
                                )
                                page = EditCategoryPage.CATEGORY
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = catColor, contentColor = Color.White),
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
                // Header Nome Sottocategoria + Squircle Icona
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
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    cursorBrush = SolidColor(catColor),
                                    decorationBox = { innerTextField ->
                                        if (editSubName.isEmpty()) {
                                            Text("Nome sottocategoria...", fontSize = 20.sp, color = TextMuted)
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(catColor)
                                    .clickable { showEditSubIconPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIconByName(editSubIcon),
                                    contentDescription = "Cambia icona sottocategoria",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // Archiviazione Sottocategoria Switch
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Archivia Sottocategoria", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Nascondi dalle scelte rapide", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = editSubArchived,
                                onCheckedChange = { editSubArchived = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = catColor,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceDark
                                )
                            )
                        }
                    }
                }

                // Pulsante Elimina Sottocategoria
                item {
                    Button(
                        onClick = {
                            targetSubForEdit?.let { target ->
                                onDeleteSubcategory(target)
                                page = EditCategoryPage.CATEGORY
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

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
}
}

    // Dialog selezione Icona & Colore a 2 schede
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

    // Dialog aggiunta sottocategoria
    if (showAddSubcategoryDialog) {
        var subName by remember { mutableStateOf("") }
        var subIcon by remember { mutableStateOf("label") }
        var showSubIconPicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddSubcategoryDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Nuova Sottocategoria", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Nome Sottocategoria") },
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
                                .background(catColor.copy(alpha = 0.20f))
                                .border(1.dp, catColor, RoundedCornerShape(10.dp))
                                .clickable { showSubIconPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIconByName(subIcon),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subName.isNotBlank()) {
                            onAddSubcategory(subName.trim(), subIcon)
                            showAddSubcategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = catColor, contentColor = Color.White)
                ) {
                    Text("Aggiungi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubcategoryDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )

        if (showSubIconPicker) {
            IconAndColorPickerDialog(
                initialIcon = subIcon,
                initialColorHex = selectedColor,
                onDismiss = { showSubIconPicker = false },
                onConfirm = { icon, _ ->
                    subIcon = icon
                }
            )
        }
    }

    if (showEditSubIconPicker) {
        IconAndColorPickerDialog(
            initialIcon = editSubIcon,
            initialColorHex = selectedColor,
            onDismiss = { showEditSubIconPicker = false },
            onConfirm = { icon, _ ->
                editSubIcon = icon
                showEditSubIconPicker = false
            }
        )
    }

    // Dialog conferma eliminazione categoria
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("Eliminare la categoria?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Tutte le transazioni associate manterranno il loro importo ma perderanno l'associazione alla categoria.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteCategory?.invoke()
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
