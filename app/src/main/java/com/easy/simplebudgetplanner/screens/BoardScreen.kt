package com.easy.simplebudgetplanner.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easy.simplebudgetplanner.data.PlanCycle
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class BoardUiState(
    val cycles: List<PlanCycle>,
    val totalIncome: Double,
    val averageIncome: Double,
    val anchorLabel: String,
    val empty: Boolean
)

fun buildBoardUiState(cycles: List<PlanCycle>): BoardUiState {
    val total = cycles.sumOf { it.income }
    val average = if (cycles.isEmpty()) 0.0 else total / cycles.size
    val anchor = cycles.firstOrNull()?.let {
        val month = monthLabel(it.month)
        "$month ${it.year}"
    } ?: "No cycles yet"
    return BoardUiState(
        cycles = cycles,
        totalIncome = total,
        averageIncome = average,
        anchorLabel = anchor,
        empty = cycles.isEmpty()
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BlueprintBoardScreen(
    state: BoardUiState,
    onCreateQuick: (String, Double) -> Unit,
    onCreateCustom: (String, Int, Int, Double) -> Unit,
    onOpenCycle: (Long) -> Unit,
    onEditCycle: (PlanCycle) -> Unit,
    onDeleteCycle: (PlanCycle) -> Unit,
    onOpenInsights: () -> Unit
) {
    var showQuick by rememberSaveable { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var cycleToEdit by rememberSaveable { mutableStateOf<PlanCycle?>(null) }
    var cycleToDelete by rememberSaveable { mutableStateOf<PlanCycle?>(null) }

    val palette = MaterialTheme.colorScheme
    val gradient = Brush.verticalGradient(
        listOf(palette.primaryContainer, palette.background)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Blueprint Board") },
                actions = {
                    IconButton(onClick = onOpenInsights) {
                        Icon(Icons.Default.AutoGraph, contentDescription = "Open insights")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = palette.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showQuick = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add cycle")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 24.dp, horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    HaloHeader(
                        totalIncome = state.totalIncome,
                        averageIncome = state.averageIncome,
                        anchorLabel = state.anchorLabel,
                        onCreateCustom = { showEditor = true; cycleToEdit = null },
                        palette = palette
                    )
                }

                if (state.empty) {
                    item {
                        EmptyTimelinePrompt(onCreate = { showQuick = true })
                    }
                } else {
                    itemsIndexed(state.cycles, key = { _, item -> item.id }) { index, cycle ->
                        TimelineCard(
                            cycle = cycle,
                            isFirst = index == 0,
                            isLast = index == state.cycles.lastIndex,
                            onOpen = { onOpenCycle(cycle.id) },
                            onEdit = {
                                cycleToEdit = cycle
                                showEditor = true
                            },
                            onDelete = { cycleToDelete = cycle }
                        )
                    }
                }
            }
        }
    }

    if (showQuick) {
        QuickCaptureDialog(
            onDismiss = { showQuick = false },
            onConfirm = { label, income ->
                onCreateQuick(label, income)
                showQuick = false
            },
            onMoreOptions = {
                showQuick = false
                cycleToEdit = null
                showEditor = true
            }
        )
    }

    if (showEditor) {
        CycleComposerSheet(
            title = if (cycleToEdit == null) "Create cycle" else "Edit cycle",
            initialLabel = cycleToEdit?.label ?: autoLabel(),
            initialYear = cycleToEdit?.year ?: Calendar.getInstance().get(Calendar.YEAR),
            initialMonth = cycleToEdit?.month ?: (Calendar.getInstance().get(Calendar.MONTH) + 1),
            initialIncome = cycleToEdit?.income ?: 0.0,
            onDismiss = { showEditor = false; cycleToEdit = null },
            onConfirm = { label, year, month, income ->
                val trimmed = label.trim()
                if (cycleToEdit == null) {
                    onCreateCustom(trimmed, year, month, income)
                } else {
                    onEditCycle(
                        cycleToEdit!!.copy(
                            label = trimmed,
                            year = year,
                            month = month,
                            income = income
                        )
                    )
                }
                showEditor = false
                cycleToEdit = null
            }
        )
    }

    cycleToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { cycleToDelete = null },
            title = { Text("Remove ${target.label.ifBlank { monthLabel(target.month) }}?") },
            text = { Text("Deleting a cycle will also remove its recorded expenses.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCycle(target)
                    cycleToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { cycleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HaloHeader(
    totalIncome: Double,
    averageIncome: Double,
    anchorLabel: String,
    onCreateCustom: () -> Unit,
    palette: ColorScheme
) {
    val haloBrush = Brush.radialGradient(
        colors = listOf(
            palette.secondaryContainer,
            palette.primaryContainer.copy(alpha = 0.3f),
            Color.Transparent
        )
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(36.dp),
        tonalElevation = 8.dp,
        color = palette.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(220.dp)
                    .background(haloBrush, shape = CircleShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Income horizon", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = currencyFormatter.format(totalIncome),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Average cycle", style = MaterialTheme.typography.labelMedium)
                        Text(
                            currencyFormatter.format(averageIncome),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Last updated", style = MaterialTheme.typography.labelMedium)
                        Text(anchorLabel, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onCreateCustom) {
                            Icon(Icons.Default.Timeline, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Plan custom cycle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimelinePrompt(onCreate: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Blueprint is blank", style = MaterialTheme.typography.titleLarge)
            Text(
                "Start by capturing your current month. You can always return to adjust month, year, or income targets later.",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onCreate) {
                Text("Create first cycle")
            }
        }
    }
}

@Composable
private fun TimelineCard(
    cycle: PlanCycle,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        TimelineGlyph(
            accent = palette.primary,
            isFirst = isFirst,
            isLast = isLast
        )
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 4.dp,
            onClick = onOpen
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = cycle.label.ifBlank { monthLabel(cycle.month) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Cycle", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${monthLabel(cycle.month)} ${cycle.year}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Income", style = MaterialTheme.typography.labelSmall)
                        Text(
                            currencyFormatter.format(cycle.income),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit cycle")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete cycle")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineGlyph(
    accent: Color,
    isFirst: Boolean,
    isLast: Boolean
) {
    val stroke = 4.dp
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 140.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val nodeRadius = 10.dp.toPx()
            val topY = 0f
            val bottomY = size.height
            val pathColor = accent.copy(alpha = 0.6f)

            if (!isFirst) {
                drawLine(
                    color = pathColor,
                    start = Offset(centerX, topY),
                    end = Offset(centerX, size.height / 2 - nodeRadius),
                    strokeWidth = stroke.toPx()
                )
            }
            if (!isLast) {
                drawLine(
                    color = pathColor,
                    start = Offset(centerX, size.height / 2 + nodeRadius),
                    end = Offset(centerX, bottomY),
                    strokeWidth = stroke.toPx()
                )
            }
            drawCircle(
                color = accent,
                radius = nodeRadius,
                center = Offset(centerX, size.height / 2)
            )
            drawCircle(
                color = Color.White,
                radius = nodeRadius / 2,
                center = Offset(centerX, size.height / 2)
            )
        }
    }
}

@Composable
private fun QuickCaptureDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit,
    onMoreOptions: () -> Unit
) {
    var label by rememberSaveable { mutableStateOf(autoLabel()) }
    var income by rememberSaveable { mutableStateOf("") }
    val palette = MaterialTheme.colorScheme
    val error by remember(income) {
        mutableStateOf(income.isNotBlank() && income.toDoubleOrNull() == null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Capture cycle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = income,
                    onValueChange = { income = it },
                    label = { Text("Income") },
                    singleLine = true,
                    isError = error
                )
                AnimatedVisibility(visible = error, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = "Enter a valid number.",
                        color = palette.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = income.toDoubleOrNull() ?: return@TextButton
                onConfirm(label, parsed)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onMoreOptions) {
                Text("More options")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleComposerSheet(
    title: String,
    initialLabel: String,
    initialYear: Int,
    initialMonth: Int,
    initialIncome: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var label by rememberSaveable { mutableStateOf(initialLabel) }
    var year by rememberSaveable { mutableStateOf(initialYear) }
    var month by rememberSaveable { mutableStateOf(initialMonth) }
    var income by rememberSaveable { mutableStateOf(initialIncome.toString()) }

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // custom drag handle (works on all material3 versions)
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = month.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull()
                        if (value != null && value in 1..12) month = value
                    },
                    label = { Text("Month (1-12)") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = year.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull()
                        if (value != null && value in 2000..2100) year = value
                    },
                    label = { Text("Year") },
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = income,
                onValueChange = { income = it },
                label = { Text("Income target") },
                singleLine = true
            )
            TextButton(onClick = {
                val picker = DatePickerDialog(
                    context,
                    { _, y, m, _ ->
                        year = y
                        month = m + 1
                    },
                    year,
                    month - 1,
                    1
                )
                picker.show()
            }) {
                Text("Pick month from calendar")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.size(12.dp))
                TextButton(onClick = {
                    val parsedIncome = income.toDoubleOrNull() ?: 0.0
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onConfirm(label, year, month, parsedIncome)
                    }
                }) {
                    Text("Save cycle")
                }
            }
        }
    }
}


private val currencyFormatter: NumberFormat
    get() = NumberFormat.getCurrencyInstance(Locale.GERMANY) // Uses Euro (€) symbol

private fun monthLabel(month: Int): String {
    val calendar = Calendar.getInstance().apply { set(Calendar.MONTH, month - 1) }
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}

private fun autoLabel(): String {
    val now = Calendar.getInstance()
    val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return format.format(now.time)
}
