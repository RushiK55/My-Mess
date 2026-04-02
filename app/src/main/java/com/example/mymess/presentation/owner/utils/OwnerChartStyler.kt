package com.example.mymess.presentation.owner.utils

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineDataSet

/**
 * Utility class to apply consistent owner panel styling to MPAndroidChart instances.
 * Ensures all charts follow the professional data-driven design with owner palette colors.
 */
object OwnerChartStyler {

    // Owner palette colors for charts
    private const val PRIMARY = "#2C3E66"    // Royal blue – data lines, primary bars
    private const val ACCENT = "#F39C12"     // Golden amber – highlighted metrics, success indicators
    private const val SUCCESS = "#2E7D32"    // Green – positive trends
    private const val INFO = "#2C7FB8"       // Info blue – neutral data points
    private const val SECONDARY = "#6C7A89"  // Steel gray – axes, labels

    /**
     * Apply base styling to any chart (shared properties).
     */
    fun styleBaseChart(chart: LineChart) {
        with(chart) {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f
            legend.textColor = Color.parseColor(SECONDARY)
            xAxis.textSize = 11f
            xAxis.textColor = Color.parseColor(SECONDARY)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.textSize = 11f
            axisLeft.textColor = Color.parseColor(SECONDARY)
            axisRight.isEnabled = false
        }
    }

    /**
     * Apply base styling to bar chart.
     */
    fun styleBaseChart(chart: BarChart) {
        with(chart) {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f
            legend.textColor = Color.parseColor(SECONDARY)
            xAxis.textSize = 11f
            xAxis.textColor = Color.parseColor(SECONDARY)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLeft.textSize = 11f
            axisLeft.textColor = Color.parseColor(SECONDARY)
            axisRight.isEnabled = false
        }
    }

    /**
     * Style a LineDataSet with owner primary color (revenue, general trends).
     */
    fun styleLineDataSet(dataSet: LineDataSet, label: String = ""): LineDataSet {
        with(dataSet) {
            color = Color.parseColor(PRIMARY)
            setCircleColor(Color.parseColor(PRIMARY))
            lineWidth = 2.5f
            circleRadius = 4f
            circleHoleRadius = 2f
            setDrawValues(false)
        }
        if (label.isNotEmpty()) dataSet.label = label
        return dataSet
    }

    /**
     * Style a LineDataSet with success color (positive growth, gains).
     */
    fun styleLineDataSetSuccess(dataSet: LineDataSet, label: String = ""): LineDataSet {
        with(dataSet) {
            color = Color.parseColor(SUCCESS)
            setCircleColor(Color.parseColor(SUCCESS))
            lineWidth = 2.5f
            circleRadius = 4f
            circleHoleRadius = 2f
            setDrawValues(false)
        }
        if (label.isNotEmpty()) dataSet.label = label
        return dataSet
    }

    /**
     * Style a LineDataSet with info color (user growth, registrations).
     */
    fun styleLineDataSetInfo(dataSet: LineDataSet, label: String = ""): LineDataSet {
        with(dataSet) {
            color = Color.parseColor(INFO)
            setCircleColor(Color.parseColor(INFO))
            lineWidth = 2.5f
            circleRadius = 4f
            circleHoleRadius = 2f
            setDrawValues(false)
        }
        if (label.isNotEmpty()) dataSet.label = label
        return dataSet
    }

    /**
     * Style a BarDataSet with owner primary color (standard metrics).
     */
    fun styleBarDataSet(dataSet: BarDataSet, label: String = ""): BarDataSet {
        with(dataSet) {
            color = Color.parseColor(PRIMARY)
            setDrawValues(false)
        }
        if (label.isNotEmpty()) dataSet.label = label
        return dataSet
    }

    /**
     * Style a BarDataSet with accent color (highlight important metrics).
     */
    fun styleBarDataSetAccent(dataSet: BarDataSet, label: String = ""): BarDataSet {
        with(dataSet) {
            color = Color.parseColor(ACCENT)
            setDrawValues(false)
        }
        if (label.isNotEmpty()) dataSet.label = label
        return dataSet
    }

    /**
     * Style a BarDataSet with success color (positive outcomes, completed orders).
     */
    fun styleBarDataSetSuccess(dataSet: BarDataSet, label: String = ""): BarDataSet {
        with(dataSet) {
            color = Color.parseColor(SUCCESS)
            setDrawValues(false)
        }
        if (label.isNotEmpty()) dataSet.label = label
        return dataSet
    }
}

