package com.liuxinyu.neurosleep.feature.home.view

// HeartRateChartManager.kt
import android.graphics.Color
import android.view.View
import com.yabu.livechart.model.DataPoint
import com.yabu.livechart.model.Dataset
import com.yabu.livechart.view.LiveChart
import com.yabu.livechart.view.LiveChartStyle
import java.util.LinkedList

class HeartRateChartManager(private val liveChart: LiveChart) {
    companion object {
        private const val TAG = "HeartRateChartManager"
        private const val SLIDING_WINDOW_SIZE = 20
        private const val INITIAL_HEART_RATE = 40f
    }

    private val slidingWindow = LinkedList<DataPoint>().apply {
        add(DataPoint(0f, INITIAL_HEART_RATE))
    }
    private val dataList = mutableListOf<DataPoint>().apply {
        add(DataPoint(0f, INITIAL_HEART_RATE))
    }

    private val chartStyle = LiveChartStyle().apply {
        mainColor = Color.RED
        pathStrokeWidth = 8f
        secondPathStrokeWidth = 4f
        textHeight = 40f
        textColor = Color.GRAY
        overlayLineColor = Color.BLUE
        overlayCircleDiameter = 32f
        overlayCircleColor = Color.GREEN
    }

    init {
        initializeChart()
    }

    private fun initializeChart() {
        liveChart.apply {
            setDataset(Dataset(dataList))
            setLiveChartStyle(chartStyle)
            drawSmoothPath()
            drawDataset()
            visibility = View.INVISIBLE
        }
    }

    fun addHeartRateData(rate: Float) {
        synchronized(this) {
            slidingWindow.add(DataPoint(0f, rate))
            while (slidingWindow.size > SLIDING_WINDOW_SIZE) {
                slidingWindow.removeFirst()
            }

            dataList.clear()
            slidingWindow.forEachIndexed { index, point ->
                dataList.add(DataPoint(index.toFloat(), point.y))
            }

            liveChart.post {
                liveChart.invalidate()
            }
        }
    }

    fun clearData() {
        synchronized(this) {
            slidingWindow.clear()
            dataList.clear()
            slidingWindow.add(DataPoint(0f, INITIAL_HEART_RATE))
            dataList.add(DataPoint(0f, INITIAL_HEART_RATE))
            liveChart.post {
                liveChart.invalidate()
            }
        }
    }

    fun setChartVisibility(visible: Boolean) {
        liveChart.post {
            liveChart.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }
}