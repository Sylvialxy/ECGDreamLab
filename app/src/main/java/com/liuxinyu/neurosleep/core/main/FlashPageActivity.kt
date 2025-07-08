package com.liuxinyu.neurosleep.core.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.liuxinyu.neurosleep.MainActivity
import com.liuxinyu.neurosleep.core.auth.LoginActivity
import com.liuxinyu.neurosleep.core.auth.RegisterActivity
import com.liuxinyu.neurosleep.databinding.ActivityFlashpageBinding

class FlashPageActivity : AppCompatActivity() {

    private lateinit var binding:ActivityFlashpageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashpageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //判断是否已经登录
        val sharedPref = getSharedPreferences("login", Context.MODE_PRIVATE)
        val loggedIn = sharedPref.getBoolean("logged", false)  // 默认值是false，表示用户未登录

        if(loggedIn) {
            // 登录成功后跳转到MainActivity，并结束FlashPageActivity
            val intent = Intent(this@FlashPageActivity, MainActivity::class.java).apply {
                // 添加标志清除之前的所有活动
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish() // 确保关闭当前活动
        } else {
            // 未登录，显示登录和注册按钮
            setupButtons()
        }
    }

    private fun setupButtons() {
        binding.btnSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}