package com.liuxinyu.neurosleep.feature.home.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.*
import kotlin.math.abs

class WaveformView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private lateinit var holder: SurfaceHolder // 声明但延迟初始化
    private var drawingThread: Job? = null
    private val drawingScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 应用 CornerPathEffect 来圆化路径的角
    private val cornerRadius = 70f // 设置圆角半径

    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 7f
        isAntiAlias = true
        style = Paint.Style.STROKE
        pathEffect = CornerPathEffect(cornerRadius)
        // 设置画笔属性并绘制路径
        strokeCap = Paint.Cap.ROUND // 圆滑的线帽
        strokeJoin = Paint.Join.ROUND
    }

    private val ecgData = mutableListOf<Float>() // 存储心电数据
    private var maxDataPoints = 20 // 显示的最大数据点数，增加以扩展时间间隔
    private var updateInterval = 100L // 每隔100毫秒更新一次，可根据需要调整
    private var horizontalSpacing = 50f // 水平间距，增大此值以扩展时间间隔

    init {
        setWillNotDraw(false)
        holder = getHolder()
        holder.setFormat(PixelFormat.TRANSPARENT)
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startDrawingLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopDrawingLoop()
    }

    private fun startDrawingLoop() {
        drawingThread = drawingScope.launch(Dispatchers.Main) {
            while (isActive) {
                drawWaveform()
                delay(updateInterval)
            }
        }
    }

    private fun stopDrawingLoop() {
        drawingThread?.cancel()
        drawingThread = null
        drawingScope.cancel()
    }

    // 将 ECG 值映射到 Y 轴坐标
    private fun mapEcgValueToY(ecgValue: Float, minY: Float, maxY: Float, minEcgValue: Float, ecgRange: Float): Float {
        return height - ((ecgValue - minEcgValue) / ecgRange * (maxY - minY) + minY)
    }

    // 根据相邻点的高度差动态调整控制点的张力
    private fun calculateControlPointTension(prevY: Float, curY: Float, nextY: Float, afterNextY: Float): Float {
        val diffPrevCur = abs(curY - prevY)
        val diffCurNext = abs(nextY - curY)
        val diffNextAfterNext = abs(afterNextY - nextY)
        val avgDiff = (diffPrevCur + diffCurNext + diffNextAfterNext) / 3f

        // 如果当前点接近水平，则增大张力；否则减小张力
        return if (avgDiff < 10f) 0.7f else 0.3f // 阈值可以根据实际情况调整
    }

    private fun drawWaveform() {
        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas() ?: return

            try {
                // 清除画布
                canvas.drawColor(Color.BLACK)
                // 绘制波形
                if (ecgData.size >= 3) { // 至少需要三个点才能绘制平滑曲线
                    val path = Path()
                    val minY = height * 0.1f
                    val maxY = height * 0.9f
                    val minEcgValue = ecgData.minOrNull() ?: 0f
                    val maxEcgValue = ecgData.maxOrNull() ?: 1f
                    val ecgRange = maxEcgValue - minEcgValue

                    // 开始绘制
                    var x0 = 0f
                    var y0 = mapEcgValueToY(ecgData[0], minY, maxY, minEcgValue, ecgRange)
                    path.moveTo(x0, y0)

                    for (i in 1 until ecgData.size) {
                        val x1 = i * horizontalSpacing
                        val y1 = mapEcgValueToY(ecgData[i], minY, maxY, minEcgValue, ecgRange)

                        if (i == 1 || i == ecgData.size - 1) {
                            // 对于第一个和最后一个点，使用简单线性连接
                            path.lineTo(x1, y1)
                        } else {
                            // 中间点使用三次贝塞尔曲线(cubic)平滑连接
                            val prevX = (i - 1) * horizontalSpacing
                            val nextX = (i + 1) * horizontalSpacing
                            val prevY = mapEcgValueToY(ecgData[i - 1], minY, maxY, minEcgValue, ecgRange)
                            val nextY = mapEcgValueToY(ecgData[i + 1], minY, maxY, minEcgValue, ecgRange)

                            // 动态调整控制点的位置以保证平滑度
                            val controlTension = calculateControlPointTension(prevY, y0, y1, nextY)
                            val controlX1 = x0 + (x1 - x0) * controlTension
                            val controlY1 = y0 + (y1 - y0) * controlTension
                            val controlX2 = x1 - (nextX - x1) * controlTension
                            val controlY2 = y1 - (nextY - y1) * controlTension

                            path.cubicTo(controlX1, controlY1, controlX2, controlY2, x1, y1)
                        }

                        // 更新起始点
                        x0 = x1
                        y0 = y1
                    }

                    // 如果数据点超出了屏幕宽度，则将路径向左平移
                    if (x0 > width) {
                        val shiftX = x0 - width + horizontalSpacing
                        val matrix = Matrix()
                        matrix.postTranslate(-shiftX, 0f)
                        path.transform(matrix)
                    }

                    // 绘制路径到 Canvas 上
                    canvas.drawPath(path, paint)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    fun addDataPoint(value: Float) {
        ecgData.add(value)
        if (ecgData.size > maxDataPoints) {
            ecgData.removeAt(0)
        }
    }

    override fun run() {}

}