package com.liuxinyu.neurosleep.feature.record

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.feature.record.adapter.ReportPagerAdapter
import com.liuxinyu.neurosleep.feature.record.viewmodel.RecordViewModel
import java.util.Calendar

/**
 * 记录页面Fragment - 显示睡眠报告和HRV报告
 */
class RecordFragment : Fragment() {

    private lateinit var viewModel: RecordViewModel

    private lateinit var btnPreviousDay: ImageView
    private lateinit var btnNextDay: ImageView
    private lateinit var tvDate: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[RecordViewModel::class.java]

        // 设置Context到ViewModel，用于日期切换时加载数据
        viewModel.setContext(requireContext())

        // 初始化视图
        initViews(view)

        // 设置ViewPager和TabLayout
        setupViewPager()

        // 设置监听器
        setupListeners()

        // 观察数据变化
        observeData()
    }

    private fun initViews(view: View) {
        btnPreviousDay = view.findViewById(R.id.btnPreviousDay)
        btnNextDay = view.findViewById(R.id.btnNextDay)
        tvDate = view.findViewById(R.id.tvDate)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

        // 为日期TextView的父容器设置点击事件
        tvDate.parent?.let { parent ->
            if (parent is View) {
                parent.setOnClickListener {
                    showDatePickerDialog()
                }
            }
        }
    }

    private fun setupViewPager() {
        val adapter = ReportPagerAdapter(this)
        viewPager.adapter = adapter

        // 关联TabLayout和ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "睡眠报告"
                1 -> "HRV报告"
                else -> ""
            }
        }.attach()
    }

    private fun setupListeners() {
        btnPreviousDay.setOnClickListener {
            viewModel.previousDay()
        }

        btnNextDay.setOnClickListener {
            viewModel.nextDay()
        }
    }

    /**
     * 显示日期选择对话框
     */
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        viewModel.currentDate.value?.let {
            calendar.time = it
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // 用户选择日期后的回调
                viewModel.setDate(selectedYear, selectedMonth, selectedDay)
            },
            year,
            month,
            day
        )

        // 设置日期选择器的最大日期为今天（不能选择未来日期）
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun observeData() {
        viewModel.currentDate.observe(viewLifecycleOwner) { date ->
            tvDate.text = viewModel.getFormattedDate()
        }
    }
}