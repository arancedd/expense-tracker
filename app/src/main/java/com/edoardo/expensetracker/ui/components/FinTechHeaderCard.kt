package com.edoardo.expensetracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.ui.theme.*

/**
 * Scheda di testata standardizzata Luxury FinTech (Ivy Wallet style).
 * Utilizzata in tutte le sezioni (Overview, Conti, Transazioni, Budget) per garantire
 * una perfetta coerenza visiva geometrica:
 * - Corner radius 24.dp
 * - Bordo hairline SurfaceBorder
 * - Gradiente verticale SurfaceElevated -> SurfaceDark
 * - Padding interno 18.dp
 * - Riga di testata con Badge e azione destra
 * - Contenuto inferiore (chips/filtri) richiudibile animatamente al scroll
 */
@Composable
fun FinTechHeaderCard(
    modifier: Modifier = Modifier,
    badgeIcon: ImageVector,
    badgeTitle: String,
    onBadgeClick: (() -> Unit)? = null,
    trailingTopContent: @Composable () -> Unit = {},
    middleContent: @Composable () -> Unit,
    bottomContent: (@Composable () -> Unit)? = null,
    bottomContentVisible: Boolean = true
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceElevated,
                            SurfaceDark.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: Badge chip e Azione a destra
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val badgeModifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .then(if (onBadgeClick != null) Modifier.clickable { onBadgeClick() } else Modifier)
                        .padding(horizontal = 10.dp, vertical = 5.dp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = badgeModifier
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = PrimaryMint,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badgeTitle.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = TextSecondary
                        )
                    }

                    trailingTopContent()
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Middle Content
                middleContent()

                // Bottom Content opzionale richiudibile animatamente
                if (bottomContent != null) {
                    AnimatedVisibility(
                        visible = bottomContentVisible,
                        enter = expandVertically(tween(220)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(220)) + fadeOut(tween(180))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            bottomContent()
                        }
                    }
                }
            }
        }
    }
}
