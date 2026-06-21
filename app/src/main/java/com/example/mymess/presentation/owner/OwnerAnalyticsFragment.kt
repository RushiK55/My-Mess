package com.example.mymess.presentation.owner

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mymess.MainActivity
import com.example.mymess.core.Resource
import com.example.mymess.data.models.OwnerAnalyticsInsights
import com.example.mymess.databinding.FragmentOwnerAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerAnalyticsFragment : Fragment() {

    private var _binding: FragmentOwnerAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerAnalyticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRefresh.setOnClickListener { viewModel.load() }
        observe()
        viewModel.load()
    }

    private fun observe() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is Resource.Error -> {
                            mainActivity?.hideLoader()
                            binding.tvInfo.text = state.message
                        }

                        Resource.Loading -> {
                            mainActivity?.showLoader("Generating analytics...")
                            binding.tvInfo.text = "Loading analytics..."
                        }

                        is Resource.Success -> {
                            mainActivity?.hideLoader()
                            bindCharts(state.data)
                        }
                    }
                }
            }
        }
    }

    private fun bindCharts(data: OwnerAnalyticsInsights) {
        binding.tvInfo.text = "Orders: ${data.summary.totalOrders} | Revenue: Rs ${data.summary.totalRevenue}"

        val orderLabels = data.ordersPerDay.map { it.label }
        val orderEntries = data.ordersPerDay.mapIndexed { index, point -> BarEntry(index.toFloat(), point.value.toFloat()) }
        val orderSet = BarDataSet(orderEntries, "Orders per day").apply { color = Color.parseColor("#1E88E5") }
        binding.chartOrders.data = BarData(orderSet)
        binding.chartOrders.xAxis.valueFormatter = IndexAxisValueFormatter(orderLabels)
        binding.chartOrders.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.chartOrders.description.isEnabled = false
        binding.chartOrders.invalidate()

        val revenueLabels = data.revenueTrend.map { it.label }
        val revenueEntries = data.revenueTrend.mapIndexed { index, point -> Entry(index.toFloat(), point.value.toFloat()) }
        val revenueSet = LineDataSet(revenueEntries, "Revenue trend").apply {
            color = Color.parseColor("#43A047")
            setCircleColor(Color.parseColor("#2E7D32"))
            lineWidth = 2f
        }
        binding.chartRevenue.data = LineData(revenueSet)
        binding.chartRevenue.xAxis.valueFormatter = IndexAxisValueFormatter(revenueLabels)
        binding.chartRevenue.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.chartRevenue.description.isEnabled = false
        binding.chartRevenue.invalidate()

        val mealLabels = data.topMeals.map { it.mealName }
        val mealEntries = data.topMeals.mapIndexed { index, item -> BarEntry(index.toFloat(), item.orders.toFloat()) }
        val mealSet = BarDataSet(mealEntries, "Most ordered meals").apply { color = Color.parseColor("#FB8C00") }
        binding.chartTopMeals.data = BarData(mealSet)
        binding.chartTopMeals.xAxis.valueFormatter = IndexAxisValueFormatter(mealLabels)
        binding.chartTopMeals.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.chartTopMeals.description.isEnabled = false
        binding.chartTopMeals.invalidate()

        val userLabels = data.userGrowth.map { it.label }
        val userEntries = data.userGrowth.mapIndexed { index, point -> Entry(index.toFloat(), point.value.toFloat()) }
        val userSet = LineDataSet(userEntries, "User growth").apply {
            color = Color.parseColor("#8E24AA")
            setCircleColor(Color.parseColor("#6A1B9A"))
            lineWidth = 2f
        }
        binding.chartUsers.data = LineData(userSet)
        binding.chartUsers.xAxis.valueFormatter = IndexAxisValueFormatter(userLabels)
        binding.chartUsers.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.chartUsers.description.isEnabled = false
        binding.chartUsers.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
