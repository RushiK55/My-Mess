package com.example.mymess.presentation.admin

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
import com.example.mymess.core.Resource
import com.example.mymess.data.models.AdminAnalyticsInsights
import com.example.mymess.databinding.FragmentAdminAnalyticsBinding
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
class AdminAnalyticsFragment : Fragment() {

    private var _binding: FragmentAdminAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminAnalyticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAdminAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRefresh.setOnClickListener { viewModel.load() }
        observe()
        viewModel.load()
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvInfo.text = state.message
                        }

                        Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.tvInfo.text = "Loading analytics..."
                        }

                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            bindCharts(state.data)
                        }
                    }
                }
            }
        }
    }

    private fun bindCharts(data: AdminAnalyticsInsights) {
        val d = data.summary
        binding.tvInfo.text = "Orders: ${d.totalOrders} | Pending: ${d.pendingOrders} | Delivered: ${d.deliveredOrders}"
        binding.tvUserGrowth.text = "Users +30d: ${d.userGrowthCount} | Total: ${d.totalUsers}"
        binding.tvOwnerRegistrations.text = "Owners +30d: ${d.ownerRegistrationsCount} | Total: ${d.totalOwners}"
        binding.tvOrderVolume.text = "Order volume this month: ${d.orderVolumeThisMonth}"
        binding.tvRevenueDistribution.text = "Revenue -> Mess: Rs ${"%.2f".format(d.messRevenue)} | Cloud: Rs ${"%.2f".format(d.cloudRevenue)}"

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
            color = Color.parseColor("#2E7D32")
            setCircleColor(Color.parseColor("#1B5E20"))
            lineWidth = 2f
        }
        binding.chartRevenue.data = LineData(revenueSet)
        binding.chartRevenue.xAxis.valueFormatter = IndexAxisValueFormatter(revenueLabels)
        binding.chartRevenue.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.chartRevenue.description.isEnabled = false
        binding.chartRevenue.invalidate()

        val sourceLabels = data.sourceRevenue.map { it.label }
        val sourceEntries = data.sourceRevenue.mapIndexed { index, point -> BarEntry(index.toFloat(), point.value.toFloat()) }
        val sourceSet = BarDataSet(sourceEntries, "Revenue by source").apply { color = Color.parseColor("#EF6C00") }
        binding.chartSourceRevenue.data = BarData(sourceSet)
        binding.chartSourceRevenue.xAxis.valueFormatter = IndexAxisValueFormatter(sourceLabels)
        binding.chartSourceRevenue.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.chartSourceRevenue.description.isEnabled = false
        binding.chartSourceRevenue.invalidate()

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

