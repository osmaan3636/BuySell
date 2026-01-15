package com.astech.buysell.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.astech.buysell.adapters.TransactionAdapter
import com.astech.buysell.databinding.FragmentReportsBinding
import com.astech.buysell.repository.ProductRepository
import kotlinx.coroutines.launch
import android.widget.Toast
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.astech.buysell.utils.CustomMarkerView
import com.astech.buysell.R

class ReportsFragment : Fragment(), OnChartValueSelectedListener {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    private val repository = ProductRepository()
    private val transactionAdapter = TransactionAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var currentTransactions: List<com.astech.buysell.models.SellTransaction> = emptyList()
    private var currentFilter = "Weekly" // "Weekly", "Monthly"
    private var chartLabels = ArrayList<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupChart()
        setupFilterButtons()
        loadDashboardData()
    }
    
    private fun setupRecyclerView() {
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }
    }
    
    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            legend.isEnabled = false
            setOnChartValueSelectedListener(this@ReportsFragment)
            
            // Set Custom Marker View
            val mv = CustomMarkerView(requireContext(), R.layout.view_marker)
            mv.chartView = this
            marker = mv
            
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = android.graphics.Color.GRAY
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "Day ${value.toInt() + 1}"
                    }
                }
            }
            
            axisLeft.apply {
                textColor = android.graphics.Color.GRAY
                setDrawGridLines(true)
                gridColor = android.graphics.Color.LTGRAY
                gridLineWidth = 0.5f
            }
            
            axisRight.isEnabled = false
        }
    }

    private fun loadDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load Transactions
            val transactions = repository.getTransactions()
            currentTransactions = transactions
            // Show only recent 5 transactions
            transactionAdapter.submitList(transactions.take(5))
            
            // Calculate Stats from Transactions
            // Filter for Today's Sales
            val sdfStats = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdfStats.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val todayStatsStr = sdfStats.format(java.util.Date())
            val todayStatsDate = parseDate(todayStatsStr)
            
            val todayTransactions = transactions.filter {
                val d = parseDate(it.createdAt)
                d == todayStatsDate
            }
            
            val totalSalesCount = todayTransactions.size
            val dailyRevenue = todayTransactions.sumOf { it.finalPrice }
            
            binding.tvTotalSales.text = totalSalesCount.toString()
            binding.tvTotalRevenue.text = "৳${dailyRevenue.toInt()}"
            // Total Profit calculation moved to updateChartData to respect filters
            
            // Calculate Daily Profit
            val todayProfit = calculateDailyProfit(transactions)
            binding.tvDailyProfit.text = "৳${todayProfit.toInt()}"
            
            // Highlight default filter button
            updateFilter("Weekly")
            
            // Calculate Daily Profit Comparison (Today vs Yesterday)
            try {
                val now = System.currentTimeMillis()
                val oneDayMillis = 86400000L
                val yesterdayProfit = transactions.filter {
                    val d = parseDate(it.createdAt)
                    // Check if date is within yesterday's range (roughly)
                    // A simpler way with our parseDate logic which returns 00:00 UTC of the day
                    
                    // Safe approach:
                    val transactionTime = parseDate(it.createdAt)
                    // Use a legacy way to get today's date formatted as string to pass to parseDate
                    val sdfLegacy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    sdfLegacy.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val todayLegacyStr = sdfLegacy.format(java.util.Date())
                    val todayMidnight = parseDate(todayLegacyStr)
                    
                    transactionTime == (todayMidnight - oneDayMillis)
                }.sumOf { it.totalProfit }
                
                val percentageChange = if (yesterdayProfit == 0.0) {
                     if (todayProfit > 0) 100.0 else 0.0
                } else {
                    ((todayProfit - yesterdayProfit) / yesterdayProfit) * 100.0
                }
                
                val sign = if (percentageChange >= 0) "+" else ""
                val formattedChange = String.format("%.0f", percentageChange)
                binding.tvDailyProfitComparison.text = "$sign$formattedChange% vs yesterday"
                
            } catch (e: Exception) {
                binding.tvDailyProfitComparison.text = "0% vs yesterday"
            }
            
            // Load Products for Stock Stats
            val products = repository.getAllProducts()
            binding.tvTotalProducts.text = products.size.toString()
            val lowStockCount = products.count { it.stock <= 5 }
            binding.tvLowStock.text = lowStockCount.toString()
            
            // Populate Chart Data
            updateChartData(transactions)
        }
    }

    private fun setupFilterButtons() {
        binding.btnFilterWeekly.setOnClickListener { updateFilter("Weekly") }
        binding.btnFilterMonthly.setOnClickListener { updateFilter("Monthly") }
    }
    
    private fun updateFilter(filter: String) {
        currentFilter = filter
        
        // Update UI buttons
        updateButtonState(binding.btnFilterWeekly, filter == "Weekly")
        updateButtonState(binding.btnFilterMonthly, filter == "Monthly")
        
        // Update Chart
        updateChartData(currentTransactions)
    }
    
    private fun updateButtonState(button: android.widget.TextView, isActive: Boolean) {
        if (isActive) {
            button.setBackgroundResource(com.astech.buysell.R.drawable.bg_rounded_segment_active)
            button.setTextColor(resources.getColor(com.astech.buysell.R.color.text_primary, null))
            button.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            button.background = null
            button.setTextColor(resources.getColor(com.astech.buysell.R.color.text_secondary, null))
            button.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun updateChartData(transactions: List<com.astech.buysell.models.SellTransaction>) {
        if (transactions.isEmpty()) return

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        
        // Prepare Date Maps for the range to ensure all x-values exist (even with 0 profit)
        val dataMap = java.util.TreeMap<String, Double>()
        val labels = ArrayList<String>()
        
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        // Normalize "today" to start of day for consistency
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        // 1. Setup Range and Labels
        if (currentFilter == "Weekly") {
            // Last 7 Days: Today - 6 days ... Today
            val endDate = calendar.timeInMillis
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -6) // Go back 6 days
            
            val labelFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            labelFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

            for (i in 0..6) {
                val dateStr = sdf.format(calendar.time)
                dataMap[dateStr] = 0.0
                labels.add(labelFormat.format(calendar.time)) // e.g. "Dec 25"
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            
        } else { // Monthly
             // Last 12 Months
             // Reset to first day of current month to act as anchor, or just last 12 months rolling?
             // "Last 12 month from db" usually implies showing T-11 months ... T (Current Month)
             
             calendar.set(java.util.Calendar.DAY_OF_MONTH, 1) // Start of this month
             
             // We need to go back 11 months
             calendar.add(java.util.Calendar.MONTH, -11)
             
             val keyFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
             keyFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
             
             val labelFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
             labelFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
             
             for (i in 0..11) {
                 val key = keyFormat.format(calendar.time)
                 dataMap[key] = 0.0
                 labels.add(labelFormat.format(calendar.time)) // e.g. "Dec"
                 calendar.add(java.util.Calendar.MONTH, 1)
             }
        }

        chartLabels = labels

        // 2. Populate Data from Transactions
        // We need to aggregate transactions into the dataMap keys
        transactions.forEach { t ->
            val dateStr = if (t.createdAt.length >= 10) t.createdAt.substring(0, 10) else ""
            if (dateStr.isNotEmpty()) {
                 if (currentFilter == "Weekly") {
                     // Key is yyyy-MM-dd
                     if (dataMap.containsKey(dateStr)) {
                         dataMap[dateStr] = (dataMap[dateStr] ?: 0.0) + t.totalProfit
                     }
                 } else {
                     // Monthly: Key is yyyy-MM
                     if (dateStr.length >= 7) {
                         val monthKey = dateStr.substring(0, 7)
                         if (dataMap.containsKey(monthKey)) {
                            dataMap[monthKey] = (dataMap[monthKey] ?: 0.0) + t.totalProfit
                         }
                     }
                 }
            }
        }
        
        // 3. Create Chart Entries
        val entries = ArrayList<com.github.mikephil.charting.data.Entry>()
        var index = 0f
        dataMap.values.forEach { profit ->
            entries.add(com.github.mikephil.charting.data.Entry(index, profit.toFloat()))
            index += 1f
        }
        
        // 4. Update Chart Style and Data
        binding.lineChart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx >= 0 && idx < labels.size) {
                    labels[idx]
                } else {
                    ""
                }
            }
        }

        val set1 = com.github.mikephil.charting.data.LineDataSet(entries, "Profit")
        set1.color = android.graphics.Color.parseColor("#5E35B1")
        set1.setCircleColor(android.graphics.Color.parseColor("#5E35B1"))
        set1.lineWidth = 2f
        set1.circleRadius = 4f
        set1.setDrawCircleHole(true)
        set1.valueTextSize = 10f
        set1.setDrawFilled(true)
        set1.fillColor = android.graphics.Color.parseColor("#7E57C2")
        set1.fillAlpha = 50
        set1.mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
        set1.setDrawValues(false)

        val data = com.github.mikephil.charting.data.LineData(set1)
        binding.lineChart.data = data

        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.invalidate()
        
        // Update Total Analysis Profit based on the visible chart data
        val totalPeriodProfit = dataMap.values.sum()
        binding.tvTotalAnalysisProfit.text = "৳${totalPeriodProfit.toInt()}"
        
        calculateAndShowProfitChange(transactions)
    }
    
    private fun calculateAndShowProfitChange(allTransactions: List<com.astech.buysell.models.SellTransaction>) {
        if (allTransactions.isEmpty()) return
        
         // Simplified change logic based on current filter
         // Weekly: This Week vs Last Week (Rolling 7 days vs Previous 7 days)
         // Monthly: This Month vs Last Month (Rolling 30 days vs Previous 30 days OR Calendar Month)
         // Let's stick to Rolling for consistency with Chart "Last 7 days" / "Last 12 months" (Last 30 days for stats?)
         // Actually for "Monthly" stats, usually it's "Last 30 Days" vs "Previous 30 Days" for the text, 
         // even if the chart shows 12 months trend.
         
         // Let's use the logic:
         // Weekly: Last 7 Days sum
         // Monthly: Last 30 Days sum (to be consistent with "Monthly" stats usually)
         
         val (currentProfit, previousProfit) = when(currentFilter) {
             "Weekly" -> {
                 val today = System.currentTimeMillis()
                 val startCurrent = today - (7 * 86400000L)
                 val startPrevious = today - (14 * 86400000L)
                 val endPrevious = today - (7 * 86400000L)
                 
                 val curr = allTransactions.filter { 
                     val d = parseDate(it.createdAt)
                     d > startCurrent 
                 }.sumOf { it.totalProfit }
                 
                 val prev = allTransactions.filter { 
                     val d = parseDate(it.createdAt)
                     d in startPrevious..endPrevious
                 }.sumOf { it.totalProfit }
                 
                 Pair(curr, prev)
             }
             else -> { // Monthly - Last 12 Months
                 val today = System.currentTimeMillis()
                 val oneYearMillis = 365L * 86400000L
                 
                 val startCurrent = today - oneYearMillis
                 val startPrevious = today - (2 * oneYearMillis)
                 val endPrevious = today - oneYearMillis
                 
                 val curr = allTransactions.filter { 
                     val d = parseDate(it.createdAt)
                     d > startCurrent 
                 }.sumOf { it.totalProfit }
                 
                 val prev = allTransactions.filter { 
                     val d = parseDate(it.createdAt)
                     d in startPrevious..endPrevious
                 }.sumOf { it.totalProfit }
                 
                 Pair(curr, prev)
             }
         }

        try {
             val percentage = if (previousProfit == 0.0) {
                 if (currentProfit > 0) 100.0 else 0.0
             } else {
                 ((currentProfit - previousProfit) / previousProfit) * 100.0
             }
             
             val isPositive = percentage >= 0
             val sign = if (isPositive) "+" else ""
             val arrow = if (isPositive) "↗" else "↘"
             val color = if (isPositive) "#2E7D32" else "#C62828" 
             val bg = if (isPositive) "#E8F5E9" else "#FFEBEE" 
             
             val formatted = String.format("%.1f", percentage)
             binding.tvProfitChange.text = "$arrow $sign$formatted%"
             binding.tvProfitChange.setTextColor(android.graphics.Color.parseColor(color))
             
             val drawable = android.graphics.drawable.GradientDrawable()
             drawable.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
             drawable.cornerRadius = 8f 
             drawable.setColor(android.graphics.Color.parseColor(bg))
             binding.tvProfitChange.background = drawable
             
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun parseDate(dateStr: String): Long {
         val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
         sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
         return try {
             if (dateStr.length >= 10) {
                val d = sdf.parse(dateStr.substring(0, 10))
                d?.time ?: 0L
             } else 0L
         } catch (e: Exception) { 0L }
    }
    
    private fun calculateDailyProfit(transactions: List<com.astech.buysell.models.SellTransaction>): Double {
        if (transactions.isEmpty()) return 0.0
        
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val today = sdf.format(java.util.Date())
            
            return transactions.filter { transaction ->
                val transactionDate = if (transaction.createdAt.length >= 10) {
                    transaction.createdAt.substring(0, 10)
                } else {
                    ""
                }
                transactionDate == today
            }.sumOf { it.totalProfit }
            
        } catch (e: Exception) {
            e.printStackTrace()
            return 0.0
        }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        // Marker view handles the display
    }

    override fun onNothingSelected() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }
}
