package com.liuxinyu.neurosleep.feature.train

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.feature.train.adapter.TrainingAdapter
import com.liuxinyu.neurosleep.feature.train.model.MediaType
import com.liuxinyu.neurosleep.feature.train.model.TrainingCategory
import com.liuxinyu.neurosleep.feature.train.player.TrainingPlayerActivity
import com.liuxinyu.neurosleep.feature.train.player.VideoPlayerActivity
import com.liuxinyu.neurosleep.feature.train.viewmodel.TrainingViewModel

class TrainFragment : Fragment() {

    private lateinit var viewModel: TrainingViewModel
    private lateinit var trainingAdapter: TrainingAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_train, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.recyclerViewTraining)

        // Setup ViewModel
        viewModel = ViewModelProvider(this)[TrainingViewModel::class.java]

        // Setup RecyclerView
        setupRecyclerView()

        // Setup TabLayout
        setupTabLayout()

        // Observe ViewModel data
        observeViewModel()
    }

    private fun setupRecyclerView() {
        trainingAdapter = TrainingAdapter(emptyList()) { trainingItem ->
            // 根据媒体类型启动不同的播放器
            when (trainingItem.mediaType) {
                MediaType.AUDIO -> {
                    // 启动音频播放器
                    val intent = Intent(requireContext(), TrainingPlayerActivity::class.java).apply {
                        putExtra(TrainingPlayerActivity.EXTRA_TRAINING_ID, trainingItem.id)
                        putExtra(TrainingPlayerActivity.EXTRA_TRAINING_TITLE, trainingItem.title)
                        putExtra(TrainingPlayerActivity.EXTRA_TRAINING_DESCRIPTION, trainingItem.description)
                        putExtra(TrainingPlayerActivity.EXTRA_TRAINING_AUDIO_RES_ID, trainingItem.mediaResId)
                        // 添加标志，确保不会创建新的任务
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                }
                MediaType.VIDEO -> {
                    // 启动视频播放器
                    val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                        putExtra(VideoPlayerActivity.EXTRA_TRAINING_ID, trainingItem.id)
                        putExtra(VideoPlayerActivity.EXTRA_TRAINING_TITLE, trainingItem.title)
                        putExtra(VideoPlayerActivity.EXTRA_TRAINING_DESCRIPTION, trainingItem.description)
                        putExtra(VideoPlayerActivity.EXTRA_TRAINING_VIDEO_RES_ID, trainingItem.mediaResId)
                        // 添加标志，确保不会创建新的任务
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                }
            }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trainingAdapter
        }
    }

    private fun setupTabLayout() {
        // Add tabs for each category
        val categories = arrayOf(
            Pair("放松训练", TrainingCategory.RELAXATION),
            Pair("激励训练", TrainingCategory.MOTIVATION),
            Pair("认知训练", TrainingCategory.COGNITIVE),
            Pair("评估测试", TrainingCategory.ASSESSMENT),
            Pair("音乐冥想", TrainingCategory.MEDITATION)
        )

        categories.forEach { (title, _) ->
            tabLayout.addTab(tabLayout.newTab().setText(title))
        }

        // Handle tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = categories[tab.position].second
                viewModel.setSelectedCategory(category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.trainingItems.observe(viewLifecycleOwner) { items ->
            trainingAdapter.updateItems(items)
        }

        viewModel.selectedCategory.observe(viewLifecycleOwner) { category ->
            // Update UI based on selected category if needed
        }
    }
}