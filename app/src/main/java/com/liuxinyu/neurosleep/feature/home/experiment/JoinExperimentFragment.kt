package com.liuxinyu.neurosleep.feature.home.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.data.network.RetrofitClient
import com.liuxinyu.neurosleep.data.network.UserApiService
import com.liuxinyu.neurosleep.util.user.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JoinExperimentFragment : Fragment() {
    
    private lateinit var studyAdapter: StudyAdapter
    private lateinit var studyRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_join_experiment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        studyRecyclerView = view.findViewById(R.id.studyRecyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)

        // 初始化RecyclerView
        setupRecyclerView()
        
        // 加载已加入的实验列表
        loadJoinedStudies()
    }
    
    private fun setupRecyclerView() {
        studyAdapter = StudyAdapter(emptyList()) { selectedStudy ->
            // 保存选中的实验 ID 和名称
            val sharedPref = requireContext().getSharedPreferences("EXPERIMENT_PREFS", android.content.Context.MODE_PRIVATE)
            sharedPref.edit().apply {
                putInt("SELECTED_EXPERIMENT_ID", selectedStudy.studyId)
                putString("SELECTED_EXPERIMENT_NAME", selectedStudy.studyName)
                apply()
            }

            android.util.Log.d("JoinExperimentFragment", "Selected study ID: ${selectedStudy.studyId}, name: ${selectedStudy.studyName}")
            Toast.makeText(requireContext(), "已选择实验: ${selectedStudy.studyName} (ID: ${selectedStudy.studyId})", Toast.LENGTH_LONG).show()

            // 返回上一页
            parentFragmentManager.popBackStack()
        }
        studyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studyAdapter
        }
    }
    
    private fun loadJoinedStudies() {
        val token = AuthManager.getFormattedToken(requireContext())
        if (token.isNullOrEmpty()) {
            showEmptyStateWithMessage("未登录\n请先登录后查看实验列表")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.getInstance(requireContext()).create(UserApiService::class.java)
                    .getJoinedStudies(token)
                
                withContext(Dispatchers.Main) {
                    if (response.code == "00000" && response.data != null) {
                        val studies = response.data
                        if (studies.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            studyAdapter.updateStudies(studies)
                        }
                    } else {
                        // 根据错误码显示不同的空状态提示
                        val emptyStateMessage = when (response.code) {
                            "A0301" -> "当前账户暂无权限查看实验列表\n请联系管理员开通权限"
                            "A0300" -> "登录已过期\n请重新登录后查看"
                            else -> "暂无加入的实验"
                        }
                        showEmptyStateWithMessage(emptyStateMessage)
                        
                        // 同时显示Toast提示
                        val toastMessage = when (response.code) {
                            "A0301" -> "权限不足，请联系管理员"
                            "A0300" -> "登录已过期，请重新登录"
                            else -> "获取实验列表失败：${response.msg}"
                        }
                        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showEmptyStateWithMessage("网络连接失败\n请检查网络后重试")
                    Toast.makeText(requireContext(), "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showEmptyState(show: Boolean) {
        if (show) {
            studyRecyclerView.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
            emptyStateText.text = "暂无加入的实验"
        } else {
            studyRecyclerView.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
        }
    }
    
    private fun showEmptyStateWithMessage(message: String) {
        studyRecyclerView.visibility = View.GONE
        emptyStateText.visibility = View.VISIBLE
        emptyStateText.text = message
    }
} 