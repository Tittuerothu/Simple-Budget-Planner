package com.easy.simplebudgetplanner.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easy.simplebudgetplanner.data.PlanCycle
import com.easy.simplebudgetplanner.ui.currencyFormatter

private data class InsightPoint(
    val label: String,
    val income: Double,
    val spent: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintInsightScreen(
    data: List<Pair<PlanCycle, Double>>,
    onBack: () -> Unit
) {
    val points = remember(data) {
        data.sortedWith(
            compareBy<Pair<PlanCycle, Double>> { it.first.year }
                .thenBy { it.first.month }
        ).map { (cycle, spent) ->
            InsightPoint(
                label = "${cycle.month.toString().padStart(2, '0')}/${cycle.year.toString().takeLast(2)}",
                income = cycle.income,
                spent = spent
            )
        }
    }
    val totals = remember(points) {
        val income = points.sumOf { it.income }
        val spent = points.sumOf { it.spent }
        income to spent
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rhythm insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Portfolio summary", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total income")
                            Text(
                                currencyFormatter.format(totals.first),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total spend")
                            Text(
                                currencyFormatter.format(totals.second),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Text(
                        text = "Trend card illustrates the difference between income goals and actual spending across recorded cycles. Drag horizontally to navigate summary cards.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            InsightChart(points = points)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(points) { point ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 160.dp)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(point.label, style = MaterialTheme.typography.titleMedium)
                            Text("Income: ${currencyFormatter.format(point.income)}", style = MaterialTheme.typography.bodySmall)
                            Text("Spent: ${currencyFormatter.format(point.spent)}", style = MaterialTheme.typography.bodySmall)
                            val delta = point.income - point.spent
                            Text(
                                text = "Balance: ${currencyFormatter.format(delta)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (delta >= 0) colorScheme.primary
                                    else colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightChart(points: List<InsightPoint>) {
    if (points.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("No data to chart yet", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }
    val palette = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.padding(24.dp)
        ) {
            val maxValue = (points.maxOf { maxOf(it.income, it.spent) } * 1.2f).toFloat().coerceAtLeast(1f)
            val chartHeight = size.height - 60f
            val chartWidth = size.width - 40f
            val startX = 20f

            val xStep = chartWidth / (points.size - 1).coerceAtLeast(1)

            // background grid
            val gridColor = palette.onSurface.copy(alpha = 0.08f)
            repeat(4) { index ->
                val y = 20f + (chartHeight / 3) * index
                drawLine(
                    color = gridColor,
                    start = Offset(startX, y),
                    end = Offset(startX + chartWidth, y),
                    strokeWidth = 2f
                )
            }

            val incomePath = Path()
            val expensePath = Path()

            points.forEachIndexed { index, point ->
                val x = startX + xStep * index
                val incomeY = 20f + chartHeight - (chartHeight * (point.income / maxValue))
                val expenseY = 20f + chartHeight - (chartHeight * (point.spent / maxValue))
                if (index == 0) {
                    incomePath.moveTo(x, incomeY.toFloat())
                    expensePath.moveTo(x, expenseY.toFloat())
                } else {
                    incomePath.lineTo(x, incomeY.toFloat())
                    expensePath.lineTo(x, expenseY.toFloat())
                }
            }

            val incomeFill = Path().apply {
                addPath(incomePath)
                val lastPoint = points.lastIndex.takeIf { it >= 0 } ?: 0
                lineTo(startX + xStep * lastPoint, 20f + chartHeight)
                lineTo(startX, 20f + chartHeight)
                close()
            }

            drawPath(
                path = incomeFill,
                brush = Brush.verticalGradient(
                    listOf(
                        palette.primary.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                style = Fill
            )

            drawPath(
                path = incomePath,
                color = palette.primary,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            drawPath(
                path = expensePath,
                color = palette.tertiary,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            points.forEachIndexed { index, point ->
                val x = startX + xStep * index
                val incomeY = 20f + chartHeight - (chartHeight * (point.income.toFloat() / maxValue))
                val expenseY = 20f + chartHeight - (chartHeight * (point.spent.toFloat() / maxValue))
                drawCircle(
                    color = palette.primary,
                    radius = 10f,
                    center = Offset(x, incomeY)
                )
                drawCircle(
                    color = palette.tertiary,
                    radius = 10f,
                    center = Offset(x, expenseY)
                )
            }

            // axis labels
            points.forEachIndexed { index, point ->
                val x = startX + xStep * index
                drawContext.canvas.nativeCanvas.drawText(
                    point.label,
                    x - 20f,
                    size.height - 12f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 28f
                    }
                )
            }
        }
    }
}

// Shared helper functions moved to SharedComposables.kt


