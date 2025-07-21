package com.liuxinyu.neurosleep.core.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.liuxinyu.neurosleep.MainActivity
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.databinding.ActivityLoginBinding
import com.liuxinyu.neurosleep.util.user.AuthManager
import com.liuxinyu.neurosleep.util.user.InputValidator
import com.liuxinyu.neurosleep.util.user.UserViewModel
import com.liuxinyu.neurosleep.util.user.UserViewModelFactory
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: UserViewModel by viewModels { UserViewModelFactory(this) }
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 自动填充记住的密码
        autoFillCredentials()

        // 密码可见性切换
        setupPasswordVisibility()

        // 登录按钮点击
        setupLoginButton()

        // 注册文本点击
        setupRegisterNavigation()

        // 忘记密码点击
        setupForgotPassword()
    }

    private fun autoFillCredentials() {
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        binding.usernameInput.setText(prefs.getString("saved_phone", ""))
        binding.loginPasswordInput.setText(prefs.getString("saved_password", ""))
        binding.rememberPasswordCheckbox.isChecked = prefs.getBoolean("remember_password", false)
    }

    private fun setupPasswordVisibility() {
        binding.visibilityIcon2.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.loginPasswordInput.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.visibilityIcon2.setImageResource(R.drawable.ic_visibility)
            } else {
                binding.loginPasswordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.visibilityIcon2.setImageResource(R.drawable.ic_visibility_off)
            }
            binding.loginPasswordInput.setSelection(binding.loginPasswordInput.text.length)
        }
    }

    private fun setupLoginButton() {
        binding.loginBtn.setOnClickListener {
            val phone = binding.usernameInput.text.toString()
            val password = binding.loginPasswordInput.text.toString()

            if (!InputValidator.isPhoneValid(phone)) {
                showToast("手机号格式错误")
                return@setOnClickListener
            }

            if (!InputValidator.isPasswordValid(password)) {
                showToast("密码格式错误")
                return@setOnClickListener
            }

            handleLogin(phone, password)
        }
    }

    private fun handleLogin(phone: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = viewModel.login(phone, password)
                if (response.code == "00000") {  // 检查 code 是否为 0
                    // 登录成功
                    val token = response.data?.token
                    if (token != null) {
                        // 保存token和手机号
                        Log.d("LoginActivity", "Saving token: ${token.take(10)}...")
                        Log.d("LoginActivity", "Full token for debugging: $token")
                        AuthManager.saveToken(this@LoginActivity, token)
                        AuthManager.savePhone(this@LoginActivity, phone)
                        // 保存记住密码设置
                        saveRememberPassword(phone, password)
                        // 跳转到主页面
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "登录失败：token为空", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 登录失败
                    Toast.makeText(this@LoginActivity, "登录失败：${response.msg}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "登录失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveRememberPassword(phone: String, password: String) {
        getSharedPreferences("login_prefs", MODE_PRIVATE).edit().apply {
            if (binding.rememberPasswordCheckbox.isChecked) {
                putString("saved_phone", phone)
                putString("saved_password", password)
            } else {
                remove("saved_phone")
                remove("saved_password")
            }
            putBoolean("remember_password", binding.rememberPasswordCheckbox.isChecked)
            apply()
        }
    }

    private fun setupRegisterNavigation() {
        binding.noAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupForgotPassword() {
        binding.forgotPassword.setOnClickListener {
            // 跳转找回密码界面（需实现）
            showToast("功能开发中，请联系管理员")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}