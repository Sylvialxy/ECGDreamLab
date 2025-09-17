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
import kotlin.math.abs

class HeartRateChartManager(
    private val context: Context,
    private val heartRateChart: LiveChart
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dataList = mutableListOf<DataPoint>()
    private val slidingWindow = LinkedList<DataPoint>()
    private val slidingWindowSize = 30 // 显示30个心率数据点，约2-3分钟的趋势
    
    // 添加数据缓冲，用于控制更新频率
    private val dataBuffer = LinkedList<Float>()
    private val bufferThreshold = 8 // 减少缓冲阈值，让心率趋势更新更及时
    
    // 添加历史心率值列表，用于计算变化率和检测异常值
    private val heartRateHistory = LinkedList<Float>()
    private val historyMaxSize = 10
    
    // 添加最大变化率限制
    private val maxChangeRate = 0.15f // 最大允许变化率15%
    
    private var currentScale = 1.0f
    private val MIN_SCALE = 0.5f
    private val MAX_SCALE = 2.0f
    private val SCALE_STEP = 0.1f
    
    private var chartType = "心率" // 默认图表类型
    
    // 各指标的初始显示范围 - 使用更合理的范围，让趋势图更美观
    private val rangeMap = mapOf(
        "心率" to Pair(50f, 100f), // 缩小心率范围，让趋势线更明显
        "meanHR" to Pair(50f, 100f),
        "RMSSD" to Pair(10f, 60f), // 更合理的RMSSD范围
        "SDNN" to Pair(20f, 80f)   // 更合理的SDNN范围
    )
    
    // 当前值范围 - 初始化为更合理的心率范围
    private var minValue = 50f
    private var maxValue = 100f
    
    // 追踪实际数据的最小值和最大值，用于自动调整范围
    private var dataMinValue = Float.MAX_VALUE
    private var dataMaxValue = Float.MIN_VALUE
    private var autoAdjustRange = true
    
    // 添加上次更新时间追踪
    private var lastUpdateTime = System.currentTimeMillis()
    private val UPDATE_INTERVAL = 2000 // 心率趋势图2秒更新一次，显示流畅的趋势变化

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
            dataBuffer.clear()
            heartRateHistory.clear()
            
            // 重置数据范围追踪
            dataMinValue = Float.MAX_VALUE
            dataMaxValue = Float.MIN_VALUE
            
            // 添加两个初始数据点，防止Bezier曲线绘制错误
            val initialValue = getInitialValueForType(chartType)
            slidingWindow.add(DataPoint(0f, initialValue))
            slidingWindow.add(DataPoint(1f, initialValue))
            
            dataList.add(DataPoint(0f, initialValue))
            dataList.add(DataPoint(1f, initialValue))
            
            // 初始化历史心率列表
            heartRateHistory.add(initialValue)
            
            // 设置数据集并应用样式
            heartRateChart.setDataset(Dataset(dataList))
                .setLiveChartStyle(getChartStyle())
                .drawSmoothPath()
                .drawYBounds() // 显示Y轴边界，让用户能看到数值范围
                .drawHorizontalGuidelines(5) // 显示5条水平网格线，便于读取数值
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
            // 将数据加入缓冲区
            dataBuffer.add(value)
            
            // 检查时间间隔和缓冲区大小，控制更新频率
            val currentTime = System.currentTimeMillis()
            if (dataBuffer.size < bufferThreshold && currentTime - lastUpdateTime < UPDATE_INTERVAL) {
                // 不满足更新条件，等待更多数据或更长时间
                return
            }
            
            // 排序缓冲区数值并使用中位数过滤异常值
            val sortedValues = dataBuffer.sorted()
            val medianValue = if (sortedValues.size % 2 == 0) {
                (sortedValues[sortedValues.size / 2 - 1] + sortedValues[sortedValues.size / 2]) / 2f
            } else {
                sortedValues[sortedValues.size / 2]
            }
            
            // 计算缓冲区中值的平均值，减少噪声，但排除最大和最小值（如果数据点足够多）
            val avgValue = if (dataBuffer.size >= 5) {
                sortedValues.subList(1, sortedValues.size - 1).average().toFloat()
            } else if (dataBuffer.isNotEmpty()) {
                dataBuffer.average().toFloat()
            } else {
                value
            }
            
            // 根据中位数和平均值的接近程度判断数据质量
            val finalValue = if (abs(medianValue - avgValue) > 10) {
                // 如果中位数和平均值相差太大，可能有异常值，优先使用中位数
                medianValue
            } else {
                // 否则使用平均值
                avgValue
            }
            
            // 应用平滑处理 - 将新值与历史数据进行加权平均
            var smoothedValue = finalValue
            if (heartRateHistory.isNotEmpty()) {
                val lastValue = heartRateHistory.last()
                // 检查变化率，如果变化过大则限制
                val changeRate = abs(finalValue - lastValue) / lastValue
                if (changeRate > maxChangeRate) {
                    // 限制变化幅度
                    val maxChange = lastValue * maxChangeRate
                    smoothedValue = if (finalValue > lastValue) {
                        lastValue + maxChange
                    } else {
                        lastValue - maxChange
                    }
                }
                
                // 应用额外的平滑处理 - 心率趋势图需要更平滑的过渡
                smoothedValue = lastValue * 0.3f + smoothedValue * 0.7f
            }
            
            // 清空缓冲区
            dataBuffer.clear()
            
            // 记录本次更新时间
            lastUpdateTime = currentTime
            
            // 更新历史心率列表
            heartRateHistory.add(smoothedValue)
            while (heartRateHistory.size > historyMaxSize) {
                heartRateHistory.removeFirst()
            }
            
            // 记录数据范围以便自动调整
            if (smoothedValue < dataMinValue) dataMinValue = smoothedValue
            if (smoothedValue > dataMaxValue) dataMaxValue = smoothedValue
            
            // 智能调整Y轴范围，让心率趋势图占据更多图表空间
            if (autoAdjustRange && heartRateHistory.size >= 5) {
                // 基于实际数据动态调整范围，让趋势线更明显
                val dataRange = dataMaxValue - dataMinValue
                val padding = kotlin.math.max(dataRange * 0.2f, 5f) // 至少5个单位的边距
                
                // 动态调整范围，但保持合理的最小范围
                val newMinValue = (dataMinValue - padding).coerceAtLeast(
                    when (chartType) {
                        "心率", "meanHR" -> 30f
                        "RMSSD" -> 0f
                        "SDNN" -> 0f
                        else -> 0f
                    }
                )
                
                val newMaxValue = dataMaxValue + padding
                
                // 确保范围不会太小，至少保持20个单位的范围
                val minRange = when (chartType) {
                    "心率", "meanHR" -> 20f
                    "RMSSD" -> 15f
                    "SDNN" -> 20f
                    else -> 20f
                }
                
                if (newMaxValue - newMinValue < minRange) {
                    val center = (newMaxValue + newMinValue) / 2f
                    minValue = center - minRange / 2f
                    maxValue = center + minRange / 2f
                } else {
                    minValue = newMinValue
                    maxValue = newMaxValue
                }
            }
            
            // 添加平滑后的值到滑动窗口
            slidingWindow.add(DataPoint(0f, smoothedValue))
            if (slidingWindow.size > slidingWindowSize) {
                slidingWindow.pollFirst()
                dataList.clear()
                for ((index, point) in slidingWindow.toList().withIndex()) {
                    dataList.add(DataPoint(index.toFloat(), point.y))
                }
            } else {
                dataList.add(DataPoint(slidingWindow.size.toFloat() - 1, smoothedValue))
            }

            // 确保数据集至少有两个点
            if (dataList.size < 2) {
                dataList.add(DataPoint(1f, smoothedValue))
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
                .drawYBounds() // 显示Y轴边界，让用户能看到数值范围
                .drawHorizontalGuidelines(5) // 显示5条水平网格线，便于读取数值
                .drawDataset()
                
            // X轴范围不需要显式设置，由数据点数量决定
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 清除所有数据并重置图表
    fun clearData() {
        slidingWindow.clear()
        dataList.clear()
        dataBuffer.clear()
        heartRateHistory.clear()
        
        // 重置数据范围追踪
        dataMinValue = Float.MAX_VALUE
        dataMaxValue = Float.MIN_VALUE
        
        setupChart()
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
    
    // 重置Y轴范围到合适的初始值
    fun resetRange() {
        val (min, max) = rangeMap[chartType] ?: Pair(50f, 100f)
        minValue = min
        maxValue = max
        
        // 重置数据范围追踪
        dataMinValue = Float.MAX_VALUE
        dataMaxValue = Float.MIN_VALUE
        
        // 更新图表显示
        updateChartBounds()
    }

    private fun getChartStyle(): LiveChartStyle {
        // 根据不同的图表类型设置不同的颜色
        val mainColor = when (chartType) {
            "心率" -> Color.rgb(220, 20, 60) // 深红色，更专业的心率颜色
            "meanHR" -> Color.rgb(255, 140, 0) // 深橙色
            "RMSSD" -> Color.rgb(34, 139, 34) // 森林绿
            "SDNN" -> Color.rgb(30, 144, 255) // 道奇蓝
            else -> Color.rgb(220, 20, 60)
        }
        
        return LiveChartStyle().apply {
            this.mainColor = mainColor
            // 心率趋势图使用较粗的线条，更容易观察趋势
            pathStrokeWidth = 4f * currentScale
            secondPathStrokeWidth = 3f * currentScale
            textHeight = 32f * currentScale
            textColor = Color.rgb(80, 80, 80) // Y轴标签文字颜色
            overlayLineColor = Color.rgb(0, 150, 255) // 蓝色覆盖线
            overlayCircleDiameter = 28f * currentScale
            overlayCircleColor = Color.rgb(0, 200, 0) // 绿色覆盖圆点
            
            // Y轴边界和网格线样式
            boundsLineColor = Color.rgb(120, 120, 120) // Y轴边界线颜色
            guideLineColor = Color.rgb(200, 200, 200) // 水平网格线颜色，较浅
            
            // 心率趋势图的专业样式设置
        }
    }
} 