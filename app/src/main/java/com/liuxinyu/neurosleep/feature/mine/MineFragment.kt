package com.liuxinyu.neurosleep.feature.mine

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.core.auth.LoginActivity
import com.liuxinyu.neurosleep.databinding.FragmentMineBinding
import com.liuxinyu.neurosleep.util.user.AuthManager

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMineBinding.inflate(inflater, container, false)

        // 设置用户信息
        val phone = AuthManager.getPhone(requireContext()) ?: "未登录"
        binding.userPhone.text = phone

        // 设置用户信息区域点击事件
        binding.userInfoLayout.setOnClickListener {
            navigateToProfile()
        }

        // 初始化RecyclerView
        val settingsList = listOf(
            MineItem(R.drawable.ic_device_management, "我的设备") {
                // 我的设备点击事件
                navigateToDeviceManagement()
            },
            MineItem(R.drawable.ic_record, "健康数据") {
                // 健康数据点击事件
                navigateToHealthData()
            },
            MineItem(R.drawable.ic_doctor_diagnosis, "医疗咨询") {
                // 医疗咨询点击事件（整合了原来的"我的问诊"）
                navigateToDoctorConsultation()
            },
            MineItem(R.drawable.ic_info, "帮助与关于") {
                // 整合帮助、反馈和关于我们
                navigateToHelpAndAbout()
            }
        )

        Log.d("SettingsFragment", "Settings list size: ${settingsList.size}")

        val adapter = MineAdapter(settingsList)
        binding.settingsRecyclerView.adapter = adapter
        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }

    private fun navigateToProfile() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_profile)
        
        // 设置对话框宽度
        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        
        // 显示当前用户手机号
        val phoneTextView = dialog.findViewById<TextView>(R.id.profile_phone)
        val currentPhone = AuthManager.getPhone(requireContext()) ?: "未登录"
        phoneTextView.text = "手机号: $currentPhone"
        
        // 切换用户按钮
        val switchUserBtn = dialog.findViewById<Button>(R.id.switch_user_btn)
        switchUserBtn.setOnClickListener {
            dialog.dismiss()
            // 跳转到登录页面进行切换用户
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }
        
        // 退出登录按钮
        val logoutBtn = dialog.findViewById<Button>(R.id.logout_btn)
        logoutBtn.setOnClickListener {
            // 清除用户信息
            AuthManager.clearAll(requireContext())
            dialog.dismiss()
            
            // 提示用户已退出
            Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
            
            // 跳转到登录页面
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish() // 关闭当前Activity
        }
        
        dialog.show()
    }

    private fun navigateToDeviceManagement() {
        Toast.makeText(requireContext(), "我的设备", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHealthData() {
        Toast.makeText(requireContext(), "健康数据", Toast.LENGTH_SHORT).show()
        // 健康数据页面现在包含"我的收藏"功能
    }

    private fun navigateToDoctorConsultation() {
        Toast.makeText(requireContext(), "医疗咨询", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHelpAndAbout() {
        Toast.makeText(requireContext(), "帮助与关于", Toast.LENGTH_SHORT).show()
       
    }

    override fun onResume() {
        super.onResume()
        // 刷新用户信息
        val phone = AuthManager.getPhone(requireContext()) ?: "未登录"
        binding.userPhone.text = phone
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
