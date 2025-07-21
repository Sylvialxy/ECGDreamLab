package com.liuxinyu.neurosleep.feature.home.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.data.network.RetrofitClient
import com.liuxinyu.neurosleep.data.network.UserApiService
import com.liuxinyu.neurosleep.util.user.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JoinExperimentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_join_experiment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inviteCodeInput = view.findViewById<TextInputEditText>(R.id.inviteCodeInput)
        val joinButton = view.findViewById<MaterialButton>(R.id.joinButton)

        joinButton.setOnClickListener {
            val inviteCode = inviteCodeInput.text.toString().trim()
            
            if (inviteCode.isEmpty()) {
                Toast.makeText(requireContext(), "请输入邀请码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 获取用户Token
            val token = AuthManager.getFormattedToken(requireContext())
            if (token.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 调用加入实验的API
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.getInstance(requireContext()).create(UserApiService::class.java)
                        .joinExperiment(inviteCode, token)
                    
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "成功加入实验", Toast.LENGTH_SHORT).show()
                            // 返回上一页
                            requireActivity().supportFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(requireContext(), "加入实验失败：${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
} 