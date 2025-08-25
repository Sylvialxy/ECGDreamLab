package com.liuxinyu.neurosleep.feature.record

import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.data.model.HrvReport

class HrvReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HRV_REPORT = "extra_hrv_report"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hrv_report)

        // 获取传递的HRV报告数据
        val hrvReport = intent.getParcelableExtra<HrvReport>(EXTRA_HRV_REPORT)
        
        if (hrvReport != null) {
            setupViews(hrvReport)
        }

        setupClickListeners()
    }

    private fun setupViews(hrvReport: HrvReport) {
        // 设置时间范围
        findViewById<TextView>(R.id.tv_time_range).text = hrvReport.timeRange

        // 设置心率数据
        findViewById<TextView>(R.id.tv_avg_heart_rate).text = "${hrvReport.avgHeartRate} bpm"
        findViewById<TextView>(R.id.tv_abnormal_heart_rate).text = hrvReport.abnormalHeartRate.toString()
        findViewById<ProgressBar>(R.id.pb_heart_rate).progress = calculateHeartRateProgress(hrvReport.avgHeartRate)

        // 设置身体状况
        findViewById<TextView>(R.id.tv_body_score).text = hrvReport.bodyScore.toString()
        findViewById<TextView>(R.id.tv_body_status).text = hrvReport.bodyStatus
        findViewById<ProgressBar>(R.id.pb_body_condition).progress = hrvReport.bodyScore

        // 设置精神状态
        findViewById<TextView>(R.id.tv_mental_score).text = hrvReport.mentalScore.toString()
        findViewById<TextView>(R.id.tv_mental_status).text = hrvReport.mentalStatus
        findViewById<ProgressBar>(R.id.pb_mental_state).progress = hrvReport.mentalScore

        // 设置压力恢复能力
        findViewById<TextView>(R.id.tv_recovery_score).text = hrvReport.recoveryScore.toString()
        findViewById<TextView>(R.id.tv_recovery_status).text = hrvReport.recoveryStatus
        findViewById<ProgressBar>(R.id.pb_recovery_ability).progress = hrvReport.recoveryScore

        // 设置抗压能力
        findViewById<TextView>(R.id.tv_resistance_score).text = hrvReport.resistanceScore.toString()
        findViewById<TextView>(R.id.tv_resistance_status).text = hrvReport.resistanceStatus
        findViewById<ProgressBar>(R.id.pb_resistance_ability).progress = hrvReport.resistanceScore

        // 设置压力值
        findViewById<TextView>(R.id.tv_stress_score).text = hrvReport.stressScore.toString()
        findViewById<TextView>(R.id.tv_stress_status).text = hrvReport.stressStatus
        findViewById<ProgressBar>(R.id.pb_stress_value).progress = hrvReport.stressScore
    }

    private fun setupClickListeners() {
        // 返回按钮
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        // 更多选项按钮
        findViewById<ImageView>(R.id.iv_more_options).setOnClickListener {
            // TODO: 显示更多选项菜单
        }

        // 设置按钮
        findViewById<ImageView>(R.id.iv_settings).setOnClickListener {
            // TODO: 打开设置页面
        }
    }

    private fun calculateHeartRateProgress(heartRate: Int): Int {
        // 根据心率值计算进度条进度
        return when {
            heartRate < 60 -> 30
            heartRate < 80 -> 60
            heartRate < 100 -> 80
            else -> 90
        }
    }
} 