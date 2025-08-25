package com.liuxinyu.neurosleep.feature.record.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.data.model.EcgRecord
import java.text.SimpleDateFormat
import java.util.*

class EcgRecordAdapter(
    private val records: List<EcgRecord>,
    private val onItemClick: (EcgRecord) -> Unit
) : RecyclerView.Adapter<EcgRecordAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMonitoringTime: TextView = view.findViewById(R.id.tv_monitoring_time)
        val tvDuration: TextView = view.findViewById(R.id.tv_duration)
        val tvHeartRate: TextView = view.findViewById(R.id.tv_heart_rate)
        val tvTotalBeats: TextView = view.findViewById(R.id.tv_total_beats)
        val tvMaxHeartRate: TextView = view.findViewById(R.id.tv_max_heart_rate)
        val tvMinHeartRate: TextView = view.findViewById(R.id.tv_min_heart_rate)
        val tvDoctorAnalysis: TextView = view.findViewById(R.id.tv_doctor_analysis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ecg_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        
        // 设置监测时间
        val timeRange = "${dateFormat.format(record.startTime)}-${dateFormat.format(record.endTime)}"
        holder.tvMonitoringTime.text = "监测时间: $timeRange"
        
        // 设置监测时长
        holder.tvDuration.text = "监测时长: ${record.duration}秒"
        
        // 设置各项指标
        holder.tvHeartRate.text = record.heartRate.toString()
        holder.tvTotalBeats.text = record.totalBeats.toString()
        holder.tvMaxHeartRate.text = record.maxHeartRate.toString()
        holder.tvMinHeartRate.text = record.minHeartRate.toString()
        
        // 设置医师分析
        holder.tvDoctorAnalysis.text = record.doctorAnalysis
        
        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClick(record)
        }
    }

    override fun getItemCount() = records.size
} 