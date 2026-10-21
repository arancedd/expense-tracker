package com.edoardo.expensetracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.edoardo.expensetracker.ui.screens.categories.PRESET_COLORS
import com.edoardo.expensetracker.ui.theme.*

/**
 * Dialog moderno a 2 schede ("Icone" e "Colori") per personalizzare categorie e conti.
 * Zero riflessi bluastri: palette 100% neutra e selettore geometricamente perfetto a raggio concentrico (12dp - 3dp = 9dp).
 */
@Composable
fun IconAndColorPickerDialog(
    initialIcon: String,
    initialColorHex: String,
    onDismiss: () -> Unit,
    onConfirm: (iconName: String, colorHex: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Icone, 1 = Colori
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColorHex) }

    val activeColor = try {
        Color(android.graphics.Color.parseColor(selectedColor))
    } catch (_: Exception) {
        PrimaryMint
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Personalizza Aspetto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher Moderno Neutro: Icone / Colori (Outer radius 12dp, padding 3dp, Inner radius 9dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    // Tab Icone
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == 0) Color(0xFF2E2E2E) else Color.Transparent)
                            .then(
                                if (selectedTab == 0) Modifier.border(1.dp, Color(0xFF444444), RoundedCornerShape(9.dp)) else Modifier
                            )
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Icone",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 0) Color.White else TextSecondary
                        )
                    }

                    // Tab Colori
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedTab == 1) Color(0xFF2E2E2E) else Color.Transparent)
                            .then(
                                if (selectedTab == 1) Modifier.border(1.dp, Color(0xFF444444), RoundedCornerShape(9.dp)) else Modifier
                            )
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Colori",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == 1) Color.White else TextSecondary
                        )
                    }
                }
            }
        },
        text = {
            Box(modifier = Modifier.height(280.dp).fillMaxWidth()) {
                if (selectedTab == 0) {
                    // Griglia Icone (catalogo completo 70+ icone)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(AVAILABLE_ICONS) { (iconKey, iconVec) ->
                            val isSelected = selectedIcon == iconKey
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.12f) else SurfaceDark)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White.copy(alpha = 0.50f) else SurfaceBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedIcon = iconKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = iconKey,
                                    tint = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Griglia Palette Colori
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(PRESET_COLORS) { colorHex ->
                            val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                            val itemColor = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (_: Exception) {
                                PrimaryMint
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(itemColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIcon, selectedColor) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Applica", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondary)
            }
        }
    )
}
