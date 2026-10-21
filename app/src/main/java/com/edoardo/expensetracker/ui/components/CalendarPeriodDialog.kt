package com.edoardo.expensetracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.edoardo.expensetracker.ui.screens.dashboard.TimePeriod
import com.edoardo.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog Calendario & Periodo avanzato (Unified per Overview e Transazioni).
 * - Chips superiori: Giorno | Settimana | Mese | Anno
 * - Header mese navigabile (< OTTOBRE 2026 >)
 * - Tocco singolo su un giorno: seleziona quel giorno specifico
 * - Pressione prolungata (Long-press) su un giorno: avvia selezione intervallo Data A -> Data B
 * - Griglia giorni mensile con evidenziazione intervallo
 * - Ripristino immediato a vista predefinita / default
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarPeriodDialog(
    currentPeriod: TimePeriod? = null,
    currentDateMillis: Long = System.currentTimeMillis(),
    selectedRangeStartMillis: Long? = null,
    selectedRangeEndMillis: Long? = null,
    onSelectPeriod: (TimePeriod) -> Unit,
    onSelectDay: (Calendar) -> Unit,
    onSelectRange: ((Long, Long) -> Unit)? = null,
    onResetToDefault: () -> Unit,
    onDismissRequest: () -> Unit,
    defaultActionLabel: String = "Oggi / Vista Default"
) {
    val initialCal = remember(currentDateMillis) {
        Calendar.getInstance(Locale.ITALY).apply {
            timeInMillis = currentDateMillis
            firstDayOfWeek = Calendar.MONDAY
        }
    }

    var viewingMonthCal by remember {
        mutableStateOf(initialCal.clone() as Calendar)
    }

    val todayCal = remember {
        Calendar.getInstance(Locale.ITALY).apply {
            firstDayOfWeek = Calendar.MONDAY
        }
    }

    var rangeStartCal by remember { mutableStateOf<Calendar?>(null) }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.ITALY) }
    val dayMonthFormat = remember { SimpleDateFormat("d MMM", Locale.ITALY) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = SurfaceElevated,
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Riga Chips Periodo: Giorno | Settimana | Mese | Anno (solo per filtri di intervallo periodo)
                if (currentPeriod != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TimePeriod.entries.forEach { period ->
                                val isSelected = currentPeriod == period && rangeStartCal == null && selectedRangeStartMillis == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) PrimaryMint.copy(alpha = 0.22f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) PrimaryMint.copy(alpha = 0.6f) else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            rangeStartCal = null
                                            onSelectPeriod(period)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = period.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryMint else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Banner modalità Intervallo A -> B (se attivo)
                if (rangeStartCal != null) {
                    Surface(
                        color = PrimaryMint.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryMint.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Intervallo: da ${dayMonthFormat.format(rangeStartCal!!.time)} → Tocca la data finale",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryMint
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Annulla intervallo",
                                tint = PrimaryMint,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { rangeStartCal = null }
                            )
                        }
                    }
                }

                // 2. Navigatore Mese: <  OTTOBRE 2026  >
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val next = viewingMonthCal.clone() as Calendar
                            next.add(Calendar.MONTH, -1)
                            viewingMonthCal = next
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Mese precedente",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = monthYearFormat.format(viewingMonthCal.time).uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = {
                            val next = viewingMonthCal.clone() as Calendar
                            next.add(Calendar.MONTH, 1)
                            viewingMonthCal = next
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Mese successivo",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Intestazione Giorni della Settimana: L M M G V S D
                val weekDayHeaders = listOf("L", "M", "M", "G", "V", "S", "D")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weekDayHeaders.forEach { dayName ->
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. Griglia Giorni Mese (7 colonne)
                val calForGrid = viewingMonthCal.clone() as Calendar
                calForGrid.set(Calendar.DAY_OF_MONTH, 1)
                val firstDayOfWeek = calForGrid.get(Calendar.DAY_OF_WEEK)
                val offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
                val daysInMonth = calForGrid.getActualMaximum(Calendar.DAY_OF_MONTH)
                val totalSlots = offset + daysInMonth
                val totalGridCells = if (totalSlots % 7 == 0) totalSlots else ((totalSlots / 7) + 1) * 7

                val isViewingCurrentMonth = viewingMonthCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        viewingMonthCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH)
                val isViewingInitialMonth = viewingMonthCal.get(Calendar.YEAR) == initialCal.get(Calendar.YEAR) &&
                        viewingMonthCal.get(Calendar.MONTH) == initialCal.get(Calendar.MONTH)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    userScrollEnabled = false,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(totalGridCells) { index ->
                        val dayNumber = index - offset + 1
                        if (index < offset || dayNumber > daysInMonth) {
                            Box(modifier = Modifier.size(32.dp))
                        } else {
                            val isToday = isViewingCurrentMonth && dayNumber == todayCal.get(Calendar.DAY_OF_MONTH)
                            val isSelectedDay = isViewingInitialMonth && dayNumber == initialCal.get(Calendar.DAY_OF_MONTH)

                            // Verifica se il giorno rientra in un intervallo
                            val thisDayCal = (viewingMonthCal.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, dayNumber)
                                set(Calendar.HOUR_OF_DAY, 12)
                            }
                            val dayMillis = thisDayCal.timeInMillis

                            val isRangeStart = rangeStartCal != null &&
                                    thisDayCal.get(Calendar.YEAR) == rangeStartCal!!.get(Calendar.YEAR) &&
                                    thisDayCal.get(Calendar.DAY_OF_YEAR) == rangeStartCal!!.get(Calendar.DAY_OF_YEAR)

                            val isInSelectedRange = selectedRangeStartMillis != null && selectedRangeEndMillis != null &&
                                    dayMillis in selectedRangeStartMillis..selectedRangeEndMillis

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isRangeStart || isSelectedDay -> PrimaryMint
                                            isInSelectedRange -> PrimaryMint.copy(alpha = 0.28f)
                                            isToday -> PrimaryMint.copy(alpha = 0.20f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isRangeStart || isSelectedDay -> PrimaryMint
                                            isInSelectedRange -> PrimaryMint.copy(alpha = 0.6f)
                                            isToday -> PrimaryMint
                                            else -> Color.Transparent
                                        },
                                        CircleShape
                                    )
                                    .then(
                                        if (onSelectRange != null) {
                                            Modifier.combinedClickable(
                                                onClick = {
                                                    val clickedCal = viewingMonthCal.clone() as Calendar
                                                    clickedCal.set(Calendar.DAY_OF_MONTH, dayNumber)

                                                    if (rangeStartCal != null) {
                                                        // Completa la selezione dell'intervallo da A a B
                                                        val start = if (rangeStartCal!!.before(clickedCal)) rangeStartCal!! else clickedCal
                                                        val end = if (rangeStartCal!!.before(clickedCal)) clickedCal else rangeStartCal!!

                                                        val startMillis = (start.clone() as Calendar).apply {
                                                            set(Calendar.HOUR_OF_DAY, 0)
                                                            set(Calendar.MINUTE, 0)
                                                            set(Calendar.SECOND, 0)
                                                            set(Calendar.MILLISECOND, 0)
                                                        }.timeInMillis

                                                        val endMillis = (end.clone() as Calendar).apply {
                                                            set(Calendar.HOUR_OF_DAY, 23)
                                                            set(Calendar.MINUTE, 59)
                                                            set(Calendar.SECOND, 59)
                                                            set(Calendar.MILLISECOND, 999)
                                                        }.timeInMillis

                                                        rangeStartCal = null
                                                        onSelectRange(startMillis, endMillis)
                                                    } else {
                                                        onSelectDay(clickedCal)
                                                    }
                                                },
                                                onLongClick = {
                                                    val start = viewingMonthCal.clone() as Calendar
                                                    start.set(Calendar.DAY_OF_MONTH, dayNumber)
                                                    rangeStartCal = start
                                                }
                                            )
                                        } else {
                                            Modifier.clickable {
                                                val clickedCal = viewingMonthCal.clone() as Calendar
                                                clickedCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                                onSelectDay(clickedCal)
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNumber",
                                    fontSize = 12.sp,
                                    fontWeight = if (isRangeStart || isSelectedDay || isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isRangeStart || isSelectedDay -> Color.Black
                                        isToday || isInSelectedRange -> PrimaryMint
                                        else -> TextPrimary
                                    }
                                )
                            }
                        }
                    }
                }

                // Suggerimento per l'intervallo solo per filtri di data
                if (onSelectRange != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tieni premuto su un giorno per filtrare da data A a data B",
                        fontSize = 10.sp,
                        color = TextMuted,
                        letterSpacing = 0.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Azioni Rapide Inferiori: Torna a Default e Chiudi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            rangeStartCal = null
                            onResetToDefault()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryMint)
                    ) {
                        Text(
                            defaultActionLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                    ) {
                        Text(
                            "Chiudi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
