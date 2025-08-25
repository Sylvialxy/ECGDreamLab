package com.liuxinyu.neurosleep.feature.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.data.model.EcgRecord
import com.liuxinyu.neurosleep.data.model.HrvReport
import com.liuxinyu.neurosleep.feature.record.adapter.EcgRecordAdapter
import java.util.*

class RecordFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EcgRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews(view)
        setupRecyclerView()
        loadMockData()
        setupClickListeners()
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.rv_records)
    }

    private fun setupRecyclerView() {
        adapter = EcgRecordAdapter(emptyList()) { record ->
            onRecordClick(record)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@RecordFragment.adapter
        }
    }

    private fun setupClickListeners() {
        view?.findViewById<ImageView>(R.id.iv_more_options)?.setOnClickListener {
            // TODO: 显示更多选项菜单
        }

        view?.findViewById<ImageView>(R.id.iv_settings)?.setOnClickListener {
            // TODO: 打开设置页面
        }
    }

    private fun loadMockData() {
        // 模拟数据
        val mockRecords = listOf(
            EcgRecord(
                id = "1",
                startTime = Date(2025, 7, 14, 21, 36, 8), // 2025/08/14 21:36:08
                endTime = Date(2025, 7, 14, 21, 36, 38),   // 2025/08/14 21:36:38
                duration = 30,
                heartRate = 72,
                totalBeats = 36,
                maxHeartRate = 85,
                minHeartRate = 65,
                doctorAnalysis = "测量时长不足 25秒,无法进行医师分析",
                hasHrvAnalysis = true
            ),
            EcgRecord(
                id = "2",
                startTime = Date(2025, 7, 14, 21, 35, 54), // 2025/08/14 21:35:54
                endTime = Date(2025, 7, 14, 21, 36, 4),    // 2025/08/14 21:36:04
                duration = 10,
                heartRate = 68,
                totalBeats = 11,
                maxHeartRate = 75,
                minHeartRate = 62,
                doctorAnalysis = "测量时长不足 25秒,无法进行医师分析",
                hasHrvAnalysis = false
            )
        )
        
        adapter = EcgRecordAdapter(mockRecords) { record ->
            onRecordClick(record)
        }
        recyclerView.adapter = adapter
    }

    private fun onRecordClick(record: EcgRecord) {
        if (record.hasHrvAnalysis) {
            // 如果有HRV分析，跳转到HRV报告页面
            val hrvReport = createMockHrvReport(record)
            val intent = Intent(context, HrvReportActivity::class.java).apply {
                putExtra(HrvReportActivity.EXTRA_HRV_REPORT, hrvReport)
            }
            startActivity(intent)
        } else {
            // 如果没有HRV分析，显示提示信息
            // TODO: 显示提示对话框
        }
    }

    private fun createMockHrvReport(record: EcgRecord): HrvReport {
        return HrvReport(
            recordId = record.id,
            timeRange = "2025/08/14 21:36:08–21:36:38",
            avgHeartRate = 72,
            abnormalHeartRate = 1,
            bodyScore = 58,
            bodyStatus = "一般",
            mentalScore = 69,
            mentalStatus = "良好",
            recoveryScore = 76,
            recoveryStatus = "良好",
            resistanceScore = 68,
            resistanceStatus = "良好",
            stressScore = 35,
            stressStatus = "轻度压力"
        )
    }
}