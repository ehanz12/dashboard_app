package com.reihan.dashboard.ui.analytics

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.ui.theme.*
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        if (state is AnalyticsState.Error) {
            snackbarHostState.showSnackbar((state as AnalyticsState.Error).message)
        }
    }

    Scaffold(
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Analytics", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. FILTER SECTION
            AnalyticsFilters(
                filterType = filterType,
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                onTypeChange = { viewModel.setFilterType(it) },
                onMonthChange = { viewModel.setMonth(it) },
                onYearChange = { viewModel.setYear(it) }
            )

            Crossfade(targetState = state, label = "StateTransition") { currentState ->
                when (currentState) {
                    is AnalyticsState.Loading -> AnalyticsSkeleton()
                    is AnalyticsState.Empty -> EmptyAnalyticsView()
                    is AnalyticsState.Error -> ErrorView(currentState.message) { viewModel.loadAnalytics() }
                    is AnalyticsState.Success -> {
                        AnalyticsContent(currentState)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsFilters(
    filterType: String,
    selectedMonth: Int,
    selectedYear: Int,
    onTypeChange: (String) -> Unit,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit
) {
    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 2..currentYear + 1).toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Segmented Button Alternative
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .padding(4.dp)
        ) {
            listOf("monthly", "yearly").forEach { type ->
                val isSelected = filterType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentBlue else Color.Transparent)
                        .clickable { onTypeChange(type) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.replaceFirstChar { it.uppercase() },
                        style = Typography.labelLarge.copy(
                            color = if (isSelected) DarkBg else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (filterType == "monthly") {
                // Month Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showMonthMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderGlass),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text(months[selectedMonth - 1])
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = showMonthMenu,
                        onDismissRequest = { showMonthMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        months.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onMonthChange(index + 1)
                                    showMonthMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Year Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showYearMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGlass),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text(selectedYear.toString())
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = showYearMenu,
                    onDismissRequest = { showYearMenu = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                onYearChange(year)
                                showYearMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsContent(state: AnalyticsState.Success) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. SUMMARY CARDS
        item {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FinanceSummaryCard(state.summary.finance)
                HabitSummaryCard(state.summary.habit)
                TaskSummaryCard(state.summary.task)
            }
        }

        // 2. FINANCE CHART
        item {
            ChartSection(title = "Finance Flow") {
                FinanceLineChart(state.finance)
            }
        }

        // 3. CATEGORY CHART
        item {
            ChartSection(title = "Expense by Category") {
                CategoryPieChart(state.categories)
            }
        }

        // 4. HABIT CHART
        item {
            ChartSection(title = "Habit Performance") {
                HabitLineChart(state.habits)
            }
        }

        // 5. TASK CHART
        item {
            ChartSection(title = "Tasks Completed") {
                TaskBarChart(state.tasks)
            }
        }

    }
}

@Composable
fun ChartSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(title, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

// --- SUMMARY CARDS ---

@Composable
fun FinanceSummaryCard(f: FinanceAnalyticsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderGlass)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finance Summary", style = Typography.labelLarge.copy(color = TextSecondary))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Income", formatRupiah(f.income), IncomeGreen)
                SummaryItem("Expense", formatRupiah(f.expense), ExpenseRed)
                SummaryItem("Balance", formatRupiah(f.balance), AccentBlue)
            }
        }
    }
}

@Composable
fun HabitSummaryCard(h: HabitAnalyticsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderGlass)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Loop, null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Habit Overview", style = Typography.labelLarge.copy(color = TextSecondary))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Today", "${h.todayCompleted}/${h.todayTotal}", TextPrimary)
                SummaryItem("Streak", "${h.currentStreak} 🔥", Color(0xFFF97316))
                SummaryItem("Rate", "${h.completionRate}%", IncomeGreen)
            }
        }
    }
}

@Composable
fun TaskSummaryCard(t: TaskAnalyticsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderGlass)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TaskAlt, null, tint = AccentTask, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tasks Overview", style = Typography.labelLarge.copy(color = TextSecondary))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Done", t.completed.toString(), IncomeGreen)
                SummaryItem("Pending", t.pending.toString(), ExpenseRed)
                SummaryItem("Rate", "${t.completionRate}%", AccentTask)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, style = Typography.bodyMedium.copy(color = TextSecondary, fontSize = 11.sp))
        Text(value, style = Typography.bodyLarge.copy(fontWeight = FontWeight.Black, color = valueColor, fontSize = 14.sp))
    }
}

// --- CHARTS ---

@Composable
fun FinanceLineChart(data: List<FinanceAnalyticsItem>) {
    if (data.isEmpty()) return
    
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setDrawGridBackground(false)
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = TextSecondary.toArgb()
                    setDrawGridLines(false)
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(data.map { it.label })
                }
                
                axisLeft.apply {
                    textColor = TextSecondary.toArgb()
                    setDrawGridLines(true)
                    gridColor = BorderGlass.toArgb()
                }
                axisRight.isEnabled = false
                
                legend.apply {
                    textColor = TextPrimary.toArgb()
                    form = Legend.LegendForm.CIRCLE
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                }
            }
        },
        update = { chart ->
            val incomeEntries = data.mapIndexed { index, item -> Entry(index.toFloat(), item.income.toFloat()) }
            val expenseEntries = data.mapIndexed { index, item -> Entry(index.toFloat(), item.expense.toFloat()) }
            
            val incomeSet = LineDataSet(incomeEntries, "Income").apply {
                color = IncomeGreen.toArgb()
                setCircleColor(IncomeGreen.toArgb())
                lineWidth = 3f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            
            val expenseSet = LineDataSet(expenseEntries, "Expense").apply {
                color = ExpenseRed.toArgb()
                setCircleColor(ExpenseRed.toArgb())
                lineWidth = 3f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            
            chart.data = LineData(incomeSet, expenseSet)
            chart.animateX(1000)
            chart.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun CategoryPieChart(data: List<CategoryAnalyticsItem>) {
    if (data.isEmpty()) return
    
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                setUsePercentValues(true)
                holeRadius = 60f
                setHoleColor(AndroidColor.TRANSPARENT)
                setCenterTextColor(TextPrimary.toArgb())
                centerText = "Expenses"
                setCenterTextSize(14f)
                setCenterTextTypeface(Typeface.DEFAULT_BOLD)
                
                legend.apply {
                    textColor = TextPrimary.toArgb()
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    setDrawInside(false)
                    isWordWrapEnabled = true
                }
            }
        },
        update = { chart ->
            val entries = data.map { PieEntry(it.amount.toFloat(), it.category) }
            val colors = listOf(AccentBlue, AccentPurple, AccentTask, AccentSchedule, IncomeGreen, Color(0xFFF97316), Color(0xFFEC4899)).map { it.toArgb() }
            
            val dataSet = PieDataSet(entries, "").apply {
                setColors(colors)
                valueTextColor = AndroidColor.WHITE
                valueTextSize = 12f
                valueFormatter = PercentFormatter(chart)
                sliceSpace = 2f
            }
            
            chart.data = PieData(dataSet)
            chart.animateY(1000)
            chart.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun HabitLineChart(data: List<HabitAnalyticsItem>) {
    if (data.isEmpty()) return
    
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = TextSecondary.toArgb()
                    setDrawGridLines(false)
                    valueFormatter = IndexAxisValueFormatter(data.map { it.label })
                }
                axisLeft.textColor = TextSecondary.toArgb()
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, item -> Entry(index.toFloat(), item.percentage.toFloat()) }
            val dataSet = LineDataSet(entries, "Habit %").apply {
                color = IncomeGreen.toArgb()
                setDrawFilled(true)
                fillColor = IncomeGreen.toArgb()
                fillAlpha = 50
                lineWidth = 2f
                setDrawValues(false)
                mode = LineDataSet.Mode.STEPPED
            }
            chart.data = LineData(dataSet)
            chart.animateX(800)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TaskBarChart(data: List<TaskAnalyticsItem>) {
    if (data.isEmpty()) return
    
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = TextSecondary.toArgb()
                    setDrawGridLines(false)
                    valueFormatter = IndexAxisValueFormatter(data.map { it.label })
                }
                axisLeft.textColor = TextSecondary.toArgb()
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, item -> BarEntry(index.toFloat(), item.completed.toFloat()) }
            val dataSet = BarDataSet(entries, "Tasks").apply {
                color = AccentTask.toArgb()
                setDrawValues(true)
                valueTextColor = TextPrimary.toArgb()
            }
            chart.data = BarData(dataSet)
            chart.animateY(1000)
        },
        modifier = Modifier.fillMaxSize()
    )
}


// --- UTILS & STATES ---

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return format.format(amount).replace("Rp", "Rp").substringBefore(",")
}

@Composable
fun AnalyticsSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface.copy(alpha = alpha))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface.copy(alpha = alpha))
        )
    }
}

@Composable
fun EmptyAnalyticsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Analytics, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Analytics Available", color = TextSecondary)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = ExpenseRed, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
