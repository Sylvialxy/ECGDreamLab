package com.liuxinyu.neurosleep.feature.home.ui

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import com.yabu.livechart.model.DataPoint
import com.yabu.livechart.model.Dataset
import com.yabu.livechart.view.LiveChart
import com.yabu.livechart.view.LiveChartStyle
import java.util.LinkedList

class HeartRateChartManager(
    private val context: Context,
    private val heartRateChart: LiveChart
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dataList = mutableListOf<DataPoint>()
    private val slidingWindow = LinkedList<DataPoint>()
    private val slidingWindowSize = 20

    private var currentScale = 1.0f
    private val MIN_SCALE = 0.5f
    private val MAX_SCALE = 2.0f
    private val SCALE_STEP = 0.1f
    
    private var chartType = "心率" // 默认图表类型
    
    // 各指标的显示范围
    private val rangeMap = mapOf(
        "心率" to Pair(40f, 150f),
        "meanHR" to Pair(40f, 150f),
        "RMSSD" to Pair(0f, 100f),
        "SDNN" to Pair(0f, 150f)
    )
    
    // 当前值范围
    private var minValue = 40f
    private var maxValue = 150f
    
    // 追踪实际数据的最小值和最大值，用于自动调整范围
    private var dataMinValue = Float.MAX_VALUE
    private var dataMaxValue = Float.MIN_VALUE
    private var autoAdjustRange = true

    init {
        setupChart()
    }
    
    // 提供访问心率图表的方法
    fun getHeartRateChart(): LiveChart {
        return heartRateChart
    }

    private fun setupChart() {
        try {
            // 初始化图表 - 确保至少有两个数据点
            slidingWindow.clear()
            dataList.clear()
            
            // 重置数据范围追踪
            dataMinValue = Float.MAX_VALUE
            dataMaxValue = Float.MIN_VALUE
            
            // 添加两个初始数据点，防止Bezier曲线绘制错误
            val initialValue = getInitialValueForType(chartType)
            slidingWindow.add(DataPoint(0f, initialValue))
            slidingWindow.add(DataPoint(1f, initialValue))
            
            dataList.add(DataPoint(0f, initialValue))
            dataList.add(DataPoint(1f, initialValue))
            
            // 设置数据集并应用样式
            heartRateChart.setDataset(Dataset(dataList))
                .setLiveChartStyle(getChartStyle())
                .drawSmoothPath()
                .drawDataset()
                
            // 设置Y轴界限
            updateChartBounds()
                
            // 默认隐藏心率图
            heartRateChart.visibility = View.INVISIBLE
        } catch (e: Exception) {
            // 捕获初始化异常
            e.printStackTrace()
        }
    }

    // 设置图表类型和相应的数值范围
    fun setChartType(type: String) {
        chartType = type
        
        // 获取该类型的值范围
        val (min, max) = rangeMap[type] ?: Pair(0f, 100f)
        minValue = min
        maxValue = max
        
        // 重置数据范围追踪
        dataMinValue = Float.MAX_VALUE
        dataMaxValue = Float.MIN_VALUE
        
        // 清除数据，重新初始化
        clearData()
    }

    fun addPoint(value: Float) {
        try {
            // 记录数据范围以便自动调整
            if (value < dataMinValue) dataMinValue = value
            if (value > dataMaxValue) dataMaxValue = value
            
            // 如果启用了自动调整范围，并且数据超出当前范围，则调整范围
            if (autoAdjustRange) {
                // 给一些余量，避免数据点贴近边缘
                if (dataMinValue < minValue + (maxValue - minValue) * 0.1f) {
                    minValue = (dataMinValue - (maxValue - minValue) * 0.1f).coerceAtLeast(0f)
                }
                if (dataMaxValue > maxValue - (maxValue - minValue) * 0.1f) {
                    maxValue = dataMaxValue + (maxValue - minValue) * 0.1f
                }
            }
            
            // 确保值在合理范围内进行显示
            val normalizedValue = value
            
            slidingWindow.add(DataPoint(0f, normalizedValue))
            if (slidingWindow.size > slidingWindowSize) {
                slidingWindow.pollFirst()
                dataList.clear()
                for ((index, point) in slidingWindow.toList().withIndex()) {
                    dataList.add(DataPoint(index.toFloat(), point.y))
                }
            } else {
                dataList.add(DataPoint(slidingWindow.size.toFloat() - 1, normalizedValue))
            }

            // 确保数据集至少有两个点
            if (dataList.size < 2) {
                dataList.add(DataPoint(1f, normalizedValue))
            }

            mainHandler.post {
                try {
                    // 直接调用updateChartBounds()来设置数据并更新图表
                    // 这样可以确保Y轴范围设置正确
                    updateChartBounds()
                    
                    heartRateChart.invalidate()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 更新图表的Y轴范围
    private fun updateChartBounds() {
        try {
            // LiveChart库可能不直接支持设置Y轴范围
            // 一种替代方案是在数据中添加两个额外的不可见点，分别为最小值和最大值
            // 这样会强制图表扩展Y轴显示范围
            
            // 先创建一个临时数据集合
            val tempDataList = mutableListOf<DataPoint>()
            
            // 添加一个最小值点（不可见，索引为-1）
            tempDataList.add(DataPoint(-1f, minValue))
            
            // 添加一个最大值点（不可见，索引为-2）
            tempDataList.add(DataPoint(-2f, maxValue))
            
            // 添加实际数据点
            tempDataList.addAll(dataList)
            
            // 更新图表
            heartRateChart.setDataset(Dataset(tempDataList))
                .setLiveChartStyle(getChartStyle())
                .drawSmoothPath()
                .drawDataset()
                
            // X轴范围不需要显式设置，由数据点数量决定
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 获取当前图表类型的初始值
    private fun getInitialValueForType(type: String): Float {
        return when (type) {
            "心率" -> 60f
            "meanHR" -> 60f
            "RMSSD" -> 30f
            "SDNN" -> 50f
            else -> 60f
        }
    }

    fun zoomIn() {
        if (currentScale < MAX_SCALE) {
            currentScale += SCALE_STEP
            applyZoom()
        }
    }

    fun zoomOut() {
        if (currentScale > MIN_SCALE) {
            currentScale -= SCALE_STEP
            applyZoom()
        }
    }

    private fun applyZoom() {
        heartRateChart.setLiveChartStyle(getChartStyle())
        heartRateChart.invalidate()
    }

    fun resetZoom() {
        currentScale = 1.0f
        applyZoom()
    }

    private fun getChartStyle(): LiveChartStyle {
        // 根据不同的图表类型设置不同的颜色
        val mainColor = when (chartType) {
            "心率" -> Color.RED
            "meanHR" -> Color.rgb(255, 165, 0) // 橙色
            "RMSSD" -> Color.rgb(0, 128, 0) // 绿色
            "SDNN" -> Color.rgb(0, 0, 255) // 蓝色
            else -> Color.RED
        }
        
        return LiveChartStyle().apply {
            this.mainColor = mainColor
            pathStrokeWidth = 8f * currentScale
            secondPathStrokeWidth = 4f * currentScale
            textHeight = 40f * currentScale
            textColor = Color.GRAY
            overlayLineColor = Color.BLUE
            overlayCircleDiameter = 32f * currentScale
            overlayCircleColor = Color.GREEN
            
            // 移除不支持的属性设置
            // LiveChartStyle不支持自定义网格线
        }
    }

    fun clearData() {
        slidingWindow.clear()
        dataList.clear()
        
        // 重置数据范围追踪
        dataMinValue = Float.MAX_VALUE
        dataMaxValue = Float.MIN_VALUE
        
        // 根据当前图表类型设置初始值
        val initialValue = getInitialValueForType(chartType)
        
        // 重置为两个初始数据点
        slidingWindow.add(DataPoint(0f, initialValue))
        slidingWindow.add(DataPoint(1f, initialValue))
        
        dataList.add(DataPoint(0f, initialValue))
        dataList.add(DataPoint(1f, initialValue))
        
        // 重设Y轴范围
        val (min, max) = rangeMap[chartType] ?: Pair(0f, 100f)
        minValue = min
        maxValue = max
        
        // 使用updateChartBounds确保正确设置Y轴范围
        updateChartBounds()
        
        heartRateChart.invalidate()
    }
} 