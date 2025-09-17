# ECG心电图实时显示改进

## 改进目标
1. 让heart_rate_ecgview滑动窗口显示更密集的数据点，包含多个R峰，使心电图更像医院报告的效果。
2. 优化real_time_heartrate显示为专业的心率趋势图（线状图），清晰展示心率变化趋势。

## 主要改进内容

### 1. 增加数据缓存容量 (EcgDisplayManager.kt)
- **滑动窗口大小**: 从10秒增加到30秒 (2000 → 6000个数据点)
- **显示数据量**: 从200个数据点增加到1000个数据点 (约5秒数据)
- **启动阈值**: 从200增加到500个数据点才开始显示，确保有足够波形

```kotlin
// 滑动窗口 保留 30s 数据 (增加从10s到30s以显示更多R峰)
if (dataQueue.size > 6000) { // 30s * 200Hz = 6000个数据点
    repeat(20) { // 每次移除20个数据点，保持流畅滚动
        dataQueue.poll()
    }
}

// 增加显示的数据量，从200增加到1000，显示约5秒的数据（包含多个R峰）
val displayDataCount = minOf(1000, filteredData.size)
for (i in filteredData.toList().takeLast(displayDataCount)) {
    dataSb.append(i.toString()).append(',')
}
```

### 2. 优化视图显示逻辑 (EcgShowView.kt)
- **移除数据长度限制**: 允许显示超过视图宽度的数据点数量
- **智能数据压缩**: 通过调整像素间隔来适应更多数据点
- **最小间隔保证**: 确保至少1像素间隔，避免数据重叠

```kotlin
// 移除数据长度限制，允许显示更多数据点以获得更密集的心电图
// 如果数据点太多，通过调整intervalRowHeart来压缩显示
data = FloatArray(dataLength)
for (i in 0 until dataLength) {
    data!![i] = java.lang.Float.parseFloat(dataStrList!![i])
}
intervalNumHeart = data!!.size

// 确保至少有足够的水平分辨率来显示密集的数据
intervalRowHeart = if (intervalNumHeart > 0) {
    maxOf(mWidth / intervalNumHeart, 1.0f) // 最小间隔为1像素
} else {
    1.0f
}
```

### 3. 改进绘制算法 (EcgShowView.kt)
- **滚动显示**: 当数据点过多时，只显示最新的数据
- **视觉优化**: 使用更鲜艳的红色 (#FF0000)，更像医院心电图
- **线条优化**: 稍微细化线条，提高精细度
- **动态范围**: 扩大数据值范围，让R峰更明显

```kotlin
// 使用更鲜艳的红色，更像医院心电图
paint!!.color = Color.parseColor("#FF0000")
paint!!.strokeWidth = mHeartLinestrokeWidth * 0.8f // 稍微细一点的线条，更精细

// 如果数据点很多，使用滚动显示最新的数据
val startIndex = if (data!!.size > mWidth.toInt() * 2) {
    data!!.size - (mWidth.toInt() * 2)
} else {
    0
}

// 限制数据值范围，但允许更大的动态范围以显示更明显的R峰
if (dataValue > 0) {
    if (dataValue > MAX_VALUE) {
        dataValue = MAX_VALUE
    }
} else {
    if (dataValue < -MAX_VALUE) {
        dataValue = -MAX_VALUE
    }
}
```

### 4. 心率计算优化
- **数据要求**: 需要至少5秒数据来准确计算心率 (从3秒增加)
- **保证准确性**: 更多数据点确保心率计算更准确

### 5. 心率趋势图优化 (HeartRateChartManager.kt)
- **专业颜色**: 使用深红色(#DC143C)等医疗级颜色方案
- **线条粗细**: 4px粗线条，便于观察心率趋势变化
- **显示窗口**: 30个数据点，约2-3分钟的心率趋势
- **更新频率**: 2秒更新一次，流畅显示趋势变化
- **数据平滑**: 优化算法减少噪声，突出真实趋势
- **智能Y轴**: 动态调整Y轴范围，让趋势线占据更多图表空间
- **Y轴显示**: 显示Y轴边界和数值标签，用户可清楚看到心率范围
- **水平网格**: 5条水平网格线，便于读取具体数值
- **美观显示**: 解决趋势图只占底部一小部分的问题

```kotlin
// 心率趋势图专业样式
val mainColor = when (chartType) {
    "心率" -> Color.rgb(220, 20, 60) // 深红色，更专业的心率颜色
    "meanHR" -> Color.rgb(255, 140, 0) // 深橙色
    "RMSSD" -> Color.rgb(34, 139, 34) // 森林绿
    "SDNN" -> Color.rgb(30, 144, 255) // 道奇蓝
}

// 心率趋势图使用较粗的线条，更容易观察趋势
pathStrokeWidth = 4f * currentScale

// Y轴显示和网格线配置
.drawYBounds() // 显示Y轴边界，让用户能看到数值范围
.drawHorizontalGuidelines(5) // 显示5条水平网格线，便于读取数值

// Y轴样式设置
textColor = Color.rgb(80, 80, 80) // Y轴标签文字颜色
boundsLineColor = Color.rgb(120, 120, 120) // Y轴边界线颜色
guideLineColor = Color.rgb(200, 200, 200) // 水平网格线颜色，较浅

// 智能Y轴范围调整 - 让趋势线占据更多图表空间
val dataRange = dataMaxValue - dataMinValue
val padding = kotlin.math.max(dataRange * 0.2f, 5f)
val newMinValue = (dataMinValue - padding).coerceAtLeast(30f) // 心率最低30
val newMaxValue = dataMaxValue + padding
```

## 预期效果

### ECG心电图波形 (real_time_ecgview)
1. **更多R峰显示**: 5秒窗口可显示4-6个心跳周期
2. **更密集数据**: 1000个数据点提供更精细的波形细节
3. **医院报告风格**: 鲜艳红色线条，密集网格背景
4. **流畅滚动**: 实时更新，保持最新数据在视图中
5. **更好的诊断价值**: 多个R峰便于观察心率变异性

### 心率趋势图 (real_time_heartrate)
1. **清晰趋势**: 线状图显示心率随时间的变化趋势
2. **专业外观**: 医疗级颜色和粗线条，易于观察
3. **平滑过渡**: 数据平滑算法减少噪声干扰
4. **合理窗口**: 2-3分钟历史数据，便于观察短期变化
5. **实时更新**: 2秒间隔更新，保持数据时效性
6. **智能缩放**: 动态调整Y轴范围，趋势线占据图表主要空间
7. **Y轴标识**: 显示Y轴边界和数值标签，用户可清楚看到心率范围
8. **网格辅助**: 5条水平网格线帮助读取具体数值
9. **美观显示**: 解决了趋势图只显示在底部一小部分的问题

## 技术参数

### ECG心电图波形
- **采样频率**: 200Hz (不变)
- **显示窗口**: 5秒 (1000个数据点)
- **缓存容量**: 30秒 (6000个数据点)  
- **更新频率**: 500ms (不变)
- **心率计算**: 基于5秒数据窗口

### 心率趋势图显示优化
- **Y轴范围**: 智能动态调整，基于实际数据
- **初始范围**: 心率50-100 BPM (替代原来的40-150)
- **自动缩放**: 数据范围+20%边距，最小保持20 BPM范围
- **最低限制**: 心率不低于30 BPM，RMSSD/SDNN不低于0
- **更新触发**: 收集5个以上历史数据点后开始智能调整
- **显示窗口**: 30个数据点，约2-3分钟历史趋势
- **更新频率**: 2秒更新一次，平滑显示变化
- **Y轴显示**: 显示边界和数值标签，便于读取心率范围
- **水平网格**: 5条网格线，辅助数值读取
- **颜色方案**: Y轴标签深灰色，网格线浅灰色，不干扰主要数据

## 兼容性

所有改进都向后兼容，不影响现有的蓝牙通信协议和数据格式。设备仍按照动态心电仪通讯协议v4.5正常工作。 