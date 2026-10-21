package com.edoardo.expensetracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.CategoryStat
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.theme.ExpenseRed
import com.edoardo.expensetracker.ui.theme.PrimaryMint
import com.edoardo.expensetracker.ui.theme.SurfaceElevated
import com.edoardo.expensetracker.ui.theme.TextPrimary
import com.edoardo.expensetracker.ui.theme.TextSecondary
import java.util.Locale

/**
 * Anello centrale moderno e sottile (FinTech segmented ring).
 * - Tratto ultrasottile (~7.5dp)
 * - Gap aumentato tra le barre (~8.0°) per segmenti divisi netti e leggibili
 * - Nessuna freccetta accanto a Spese/Entrate
 * - Scambio animato dei numeri tra centro e pillola al tap
 * - Geometria fissa a 190dp per evitare qualsiasi spostamento o oscillazione di layout
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OneMoneyDonutChart(
    categoryStats: List<CategoryStat>,
    totalAmount: Double,
    secondaryAmount: Double,
    currentType: TransactionType = TransactionType.EXPENSE,
    onRingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categoryStats, totalAmount, currentType) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    val isExpense = currentType == TransactionType.EXPENSE
    val targetPrimaryColor = if (isExpense) {
        if (totalAmount > 0) ExpenseRed else TextPrimary
    } else {
        if (totalAmount > 0) PrimaryMint else TextPrimary
    }
    val animPrimaryColor by animateColorAsState(targetPrimaryColor, animationSpec = tween(350), label = "donut_primary_color")

    val targetLabelColor = if (isExpense) ExpenseRed.copy(alpha = 0.9f) else PrimaryMint.copy(alpha = 0.9f)
    val animLabelColor by animateColorAsState(targetLabelColor, animationSpec = tween(350), label = "donut_label_color")

    Box(
        modifier = modifier
            .size(182.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onRingClick() },
        contentAlignment = Alignment.Center
    ) {
        // Disco centrale decorativo con contorno sottile che abbraccia da vicino le barre
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val strokeWidth = 6.dp.toPx()
            // Diametro ottimizzato per posizionare le barre immediatamente adiacenti al cerchio centrale
            val diameter = 162.dp.toPx()
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

            if (totalAmount > 0 && categoryStats.isNotEmpty()) {
                var currentAngle = -90f
                val count = categoryStats.size
                // Spazio divisore tra le barre (7 gradi bilanciati)
                val gapAngle = if (count > 1) 7.0f else 0f

                categoryStats.forEach { stat ->
                    val rawSweep = ((stat.totalAmount / totalAmount) * 360f).toFloat() * animationProgress.value
                    val sliceColor = try {
                        Color(android.graphics.Color.parseColor(stat.colorHex))
                    } catch (_: Exception) {
                        if (isExpense) ExpenseRed else PrimaryMint
                    }

                    if (rawSweep > gapAngle + 1f) {
                        val sweep = rawSweep - gapAngle
                        drawArc(
                            color = sliceColor,
                            startAngle = currentAngle + (gapAngle / 2f),
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    } else if (rawSweep > 1f) {
                        drawArc(
                            color = sliceColor,
                            startAngle = currentAngle,
                            sweepAngle = rawSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    currentAngle += rawSweep
                }
            }
        }

        // Testo centrale interattivo con animazione di switch
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // Label tipo (SPESE / ENTRATE) pulita senza freccette
            AnimatedContent(
                targetState = isExpense,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInVertically { it / 2 })
                        .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / 2 })
                },
                contentAlignment = Alignment.Center,
                label = "label_type"
            ) { expense ->
                Text(
                    text = if (expense) "SPESE" else "ENTRATE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    color = animLabelColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Cifra principale animata (scambio con la pillola)
            AnimatedContent(
                targetState = Pair(isExpense, totalAmount),
                transitionSpec = {
                    if (targetState.first != initialState.first) {
                        (slideInVertically(animationSpec = tween(320)) { it } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutVertically(animationSpec = tween(280)) { -it } + fadeOut())
                    } else {
                        fadeIn(tween(200)).togetherWith(fadeOut(tween(200)))
                    }
                },
                contentAlignment = Alignment.Center,
                label = "primary_amount"
            ) { (_, amount) ->
                Text(
                    text = "€ ${String.format(Locale.ITALY, "%,.0f", amount)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = animPrimaryColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Micro pillola: chip con colore morphing fluido tra verde (uscite) e rosso (entrate)
            val targetPillBg = if (isExpense) PrimaryMint.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f)
            val targetPillBorder = if (isExpense) PrimaryMint.copy(alpha = 0.30f) else ExpenseRed.copy(alpha = 0.30f)
            val targetPillText = if (isExpense) PrimaryMint else ExpenseRed

            val pillBgColor by animateColorAsState(targetPillBg, animationSpec = tween(350), label = "donut_pill_bg")
            val pillBorderColor by animateColorAsState(targetPillBorder, animationSpec = tween(350), label = "donut_pill_border")
            val pillTextColor by animateColorAsState(targetPillText, animationSpec = tween(350), label = "donut_pill_text")

            AnimatedContent(
                targetState = Pair(isExpense, secondaryAmount),
                transitionSpec = {
                    if (targetState.first != initialState.first) {
                        (slideInVertically(animationSpec = tween(320)) { -it } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutVertically(animationSpec = tween(280)) { it } + fadeOut())
                    } else {
                        fadeIn(tween(200)).togetherWith(fadeOut(tween(200)))
                    }
                },
                contentAlignment = Alignment.Center,
                label = "pill_amount"
            ) { (_, secAmount) ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pillBgColor)
                        .border(1.dp, pillBorderColor, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "€ ${String.format(Locale.ITALY, "%,.0f", secAmount)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pillTextColor
                    )
                }
            }
        }
    }
}
