package com.edoardo.expensetracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.ui.navigation.Screen
import com.edoardo.expensetracker.ui.theme.PrimaryMint
import com.edoardo.expensetracker.ui.theme.TextMuted

/**
 * Barra di navigazione fluttuante ultra-smooth (Liquid Glass Dock style con Pillola Scorrevole).
 * - Pillola attiva che scivola con fisica a molla (spring animation) sotto la scheda selezionata
 * - Bounce subtle dell'icona (scale animation)
 * - Translucenza liquid glass profonda con riflesso superiore e ombra morbida
 */
@Composable
fun FloatingGlassNavBar(
    selectedScreen: Screen,
    screens: List<Screen>,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = screens.indexOf(selectedScreen).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(29.dp), spotColor = Color.Black.copy(alpha = 0.65f)),
            shape = RoundedCornerShape(29.dp),
            color = Color(0xF5141414),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                val totalWidth = maxWidth
                val slotWidth = totalWidth / screens.size

                val animatedIndicatorOffset by animateDpAsState(
                    targetValue = slotWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "nav_pill_slide"
                )

                // 1. Pillola attiva scorrevole (Liquid Pill con glow) con ampio respiro
                Box(
                    modifier = Modifier
                        .offset(x = animatedIndicatorOffset)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryMint.copy(alpha = 0.22f),
                                    PrimaryMint.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryMint.copy(alpha = 0.50f),
                                    PrimaryMint.copy(alpha = 0.20f)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                )

                // 2. Icone pure centrate (Senza etichette di testo)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    screens.forEach { screen ->
                        val isSelected = selectedScreen == screen

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) PrimaryMint else TextMuted,
                            animationSpec = tween(220),
                            label = "nav_color"
                        )

                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.16f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = 0.62f,
                                stiffness = 420f
                            ),
                            label = "nav_scale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onScreenSelected(screen) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                        }
                    }
                }
            }
        }
    }
}
