package com.liuxinyu.neurosleep.feature.record

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

    private fun observeData() {
        viewModel.currentDate.observe(viewLifecycleOwner) { date ->
            tvDate.text = viewModel.getFormattedDate()
        }
    }
}