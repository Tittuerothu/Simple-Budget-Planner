package com.easy.simplebudgetplanner.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.easy.simplebudgetplanner.data.CycleLedger
import com.easy.simplebudgetplanner.data.PlanTransaction
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintPeriodScreen(
    ledger: CycleLedger?,
    onNavigateUp: () -> Unit,
    onEditIncome: (Double) -> Unit,
    onUpdateCycle: (String, Int, Int) -> Unit,
    onAddTransaction: (String, Double, String, Long) -> Unit,
    onUpdateTransaction: (PlanTransaction) -> Unit,
    onDeleteTransaction: (PlanTransaction) -> Unit,
    onOpenInsights: () -> Unit
) {
    if (ledger == null) {
        MissingLedger(onNavigateUp = onNavigateUp)
        return
    }

    var showIncomeDialog by rememberSaveable { mutableStateOf(false) }
    var showMetaSheet by rememberSaveable { mutableStateOf(false) }
    var editingTransaction by rememberSaveable { mutableStateOf<PlanTransaction?>(null) }
    var creatingTransaction by rememberSaveable { mutableStateOf(false) }
    var deletingTransaction by rememberSaveable { mutableStateOf<PlanTransaction?>(null) }

    val palette = MaterialTheme.colorScheme
    val backdrop = remember(palette) {
        Brush.verticalGradient(
            listOf(
                palette.secondaryContainer.copy(alpha = 0.6f),
                palette.background
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = ledger.cycle.label.ifBlank { "${monthLabel(ledger.cycle.month)} ${ledger.cycle.year}" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMetaSheet = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit cycle")
                    }
                    IconButton(onClick = onOpenInsights) {
                        Icon(Icons.Default.AutoGraph, contentDescription = "Open insights")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creatingTransaction = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdrop)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    LedgerHeader(
                        income = ledger.cycle.income,
                        spent = ledger.totalSpent,
                        onEditIncome = { showIncomeDialog = true }
                    )
                }
                item {
                    HighlightsCluster(ledger = ledger)
                }
                if (ledger.transactions.isEmpty()) {
                    item { EmptyTransactionsHint() }
                } else {
                    items(ledger.transactions, key = { it.id }) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onEdit = { editingTransaction = transaction },
                            onDelete = { deletingTransaction = transaction }
                        )
                    }
                }
            }
        }
    }

    if (showIncomeDialog) {
        IncomeAdjustDialog(
            current = ledger.cycle.income,
            onDismiss = { showIncomeDialog = false },
            onConfirm = {
                onEditIncome(it)
                showIncomeDialog = false
            }
        )
    }

    if (showMetaSheet) {
        CycleMetaSheet(
            label = ledger.cycle.label,
            year = ledger.cycle.year,
            month = ledger.cycle.month,
            onDismiss = { showMetaSheet = false },
            onConfirm = { label, year, month ->
                onUpdateCycle(label, year, month)
                showMetaSheet = false
            }
        )
    }

    if (creatingTransaction) {
        TransactionSheet(
            title = "Log expense",
            onDismiss = { creatingTransaction = false },
            onConfirm = { title, amount, category, millis ->
                onAddTransaction(title, amount, category, millis)
                creatingTransaction = false
            }
        )
    }

    editingTransaction?.let { transaction ->
        TransactionSheet(
            title = "Edit expense",
            initialTitle = transaction.title,
            initialAmount = transaction.amount,
            initialCategory = transaction.category,
            initialMillis = transaction.spentAt,
            onDismiss = { editingTransaction = null },
            onConfirm = { title, amount, category, millis ->
                onUpdateTransaction(
                    transaction.copy(
                        title = title,
                        amount = amount,
                        category = category,
                        spentAt = millis
                    )
                )
                editingTransaction = null
            }
        )
    }

    deletingTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text("Delete ${transaction.title}?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTransaction(transaction)
                    deletingTransaction = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransaction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingLedger(onNavigateUp: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Blueprint ledger") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text("This cycle was removed.", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onNavigateUp) {
                Text("Back to board")
            }
        }
    }
}

@Composable
private fun LedgerHeader(
    income: Double,
    spent: Double,
    onEditIncome: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (income == 0.0) 0f else (spent / income).toFloat().coerceIn(0f, 1.2f),
        label = "spendProgress"
    )
    val displayProgress = progress.coerceIn(0f, 1f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colorScheme.primaryContainer,
                            colorScheme.tertiaryContainer
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Income target", style = MaterialTheme.typography.labelMedium)
                    Text(
                        currencyFormatter.format(income),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                TextButton(onClick = onEditIncome) {
                    Icon(Icons.Default.Savings, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Adjust")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Spent so far")
                    Text(currencyFormatter.format(spent))
                }
                LinearProgressIndicator(
                    progress = displayProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    trackColor = colorScheme.surfaceVariant,
                    color = if (spent <= income) colorScheme.primary else colorScheme.error
                )
                AnimatedVisibility(visible = spent > income) {
                    Text(
                        "You have exceeded the target income.",
                        color = colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightsCluster(ledger: CycleLedger) {
    val largest = ledger.transactions.maxByOrNull { it.amount }
    val average = if (ledger.transactions.isEmpty()) 0.0 else ledger.totalSpent / ledger.transactions.size
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Highlights", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HighlightBadge(title = "Transactions", value = ledger.transactions.size.toString())
                HighlightBadge(
                    title = "Largest",
                    value = largest?.let { currencyFormatter.format(it.amount) } ?: "--"
                )
                HighlightBadge(
                    title = "Average",
                    value = currencyFormatter.format(average)
                )
            }
        }
    }
}

@Composable
private fun HighlightBadge(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun TransactionCard(
    transaction: PlanTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(transaction.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        expenseDate(transaction.spentAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        currencyFormatter.format(transaction.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (transaction.category.isNotBlank()) {
                        Text(transaction.category, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit expense")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete expense")
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("No expenses yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Press the add button to record your first transaction. Each entry captures a title, amount, optional category, and date.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun IncomeAdjustDialog(
    current: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var input by rememberSaveable { mutableStateOf(current.toString()) }
    val error = input.isNotBlank() && input.toDoubleOrNull() == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust income target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Income") },
                    singleLine = true,
                    isError = error
                )
                AnimatedVisibility(visible = error) {
                    Text(
                        "Please enter a valid number.",
                        color = colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = input.toDoubleOrNull() ?: return@TextButton
                onConfirm(parsed)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleMetaSheet(
    label: String,
    year: Int,
    month: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(label) }
    var yearState by rememberSaveable { mutableStateOf(year) }
    var monthState by rememberSaveable { mutableStateOf(month) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // custom drag handle (works on all Material3 versions)
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
                        .background(colorScheme.onSurface.copy(alpha = 0.12f), shape = RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Cycle details", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Label") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = monthState.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull()
                        if (value != null && value in 1..12) monthState = value
                    },
                    label = { Text("Month") },
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = yearState.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull()
                        if (value != null && value in 2000..2100) yearState = value
                    },
                    label = { Text("Year") },
                    singleLine = true
                )
            }
            TextButton(onClick = {
                val picker = DatePickerDialog(
                    context,
                    { _, y, m, _ ->
                        yearState = y
                        monthState = m + 1
                    },
                    yearState,
                    monthState - 1,
                    1
                )
                picker.show()
            }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Pick month from calendar")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.size(12.dp))
                TextButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onConfirm(name, yearState, monthState)
                    }
                }) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionSheet(
    title: String,
    initialTitle: String = "",
    initialAmount: Double = 0.0,
    initialCategory: String = "",
    initialMillis: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initialTitle) }
    var amount by rememberSaveable { mutableStateOf(initialAmount.toString()) }
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var millis by rememberSaveable { mutableStateOf(initialMillis) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // custom drag handle (works on all Material3 versions)
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
                        .background(colorScheme.onSurface.copy(alpha = 0.12f), shape = RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title") },
                singleLine = true
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                singleLine = true
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (optional)") },
                singleLine = true
            )
            TextButton(onClick = {
                val calendar = Calendar.getInstance().apply { timeInMillis = millis }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        calendar.set(y, m, d, 0, 0, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        millis = calendar.timeInMillis
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Text("Date: ${expenseDate(millis)}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.size(12.dp))
                TextButton(onClick = {
                    val parsed = amount.toDoubleOrNull() ?: return@TextButton
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onConfirm(name, parsed, category, millis)
                    }
                }) {
                    Text("Save")
                }
            }
        }
    }
}

private val currencyFormatter: NumberFormat
    get() = NumberFormat.getCurrencyInstance()

private fun expenseDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(millis)
}

private fun monthLabel(month: Int): String {
    val calendar = Calendar.getInstance().apply { set(Calendar.MONTH, month - 1) }
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}
