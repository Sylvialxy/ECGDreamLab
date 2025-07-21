package com.liuxinyu.neurosleep.core.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.databinding.ActivityRegisterBinding
import com.liuxinyu.neurosleep.data.network.ApiResponse
import com.liuxinyu.neurosleep.util.user.InputValidator
import com.liuxinyu.neurosleep.util.user.UserViewModel
import com.liuxinyu.neurosleep.util.user.UserViewModelFactory
import kotlinx.coroutines.launch


class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: UserViewModel by viewModels { UserViewModelFactory(this) }
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 密码可见性切换
        setupPasswordVisibility()

        // 获取验证码按钮点击
        setupVerificationCode()

        // 注册按钮点击
        setupRegisterButton()
    }

    private fun setupPasswordVisibility() {
        // 密码可见性切换
        binding.visibilityIcon.setOnClickListener {
            togglePasswordVisibility(
                passwordEditText = binding.passwordInput,
                visibilityIcon = binding.visibilityIcon,
                isVisible = isPasswordVisible
            )
            isPasswordVisible = !isPasswordVisible
        }

        // 确认密码可见性切换
        binding.visibilityIcon2.setOnClickListener {
            togglePasswordVisibility(
                passwordEditText = binding.confirmPasswordInput,
                visibilityIcon = binding.visibilityIcon2,
                isVisible = isConfirmPasswordVisible
            )
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }
    }

    private fun togglePasswordVisibility(
        passwordEditText: EditText,
        visibilityIcon: ImageView,
        isVisible: Boolean
    ) {
        if (isVisible) {
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            visibilityIcon.setImageResource(R.drawable.ic_visibility_off)
        } else {
            passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            visibilityIcon.setImageResource(R.drawable.ic_visibility)
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun setupVerificationCode() {
        binding.getVerificationCodeBtn.setOnClickListener {
            val phone = binding.phoneInput.text.toString()

            if (!InputValidator.isPhoneValid(phone)) {
                showToast("请输入有效的手机号")
                return@setOnClickListener
            }

            // 发送验证码逻辑（需补充后端接口）
            startCountDownTimer()
        }
    }

    private fun startCountDownTimer() {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.getVerificationCodeBtn.text = "${millisUntilFinished / 1000}秒后重试"
                binding.getVerificationCodeBtn.isEnabled = false
            }

            override fun onFinish() {
                binding.getVerificationCodeBtn.text = "获取验证码"
                binding.getVerificationCodeBtn.isEnabled = true
            }
        }.start()
    }

    private fun setupRegisterButton() {
        binding.registerBtn.setOnClickListener {
            val phone = binding.phoneInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()
            val verificationCode = binding.verificationCodeInput.text.toString()

            // 输入验证
            if (!checkInputValidity(phone, password, confirmPassword, verificationCode)) return@setOnClickListener

            lifecycleScope.launch {
                val response = viewModel.register(phone, password)
                handleRegisterResult(response)
            }
        }
    }

    private fun checkInputValidity(
        phone: String,
        password: String,
        confirmPassword: String,
        verificationCode: String
    ): Boolean {
        if (!binding.termsCheckbox.isChecked) {
            showToast("请同意服务条款")
            return false
        }

        if (!InputValidator.isPhoneValid(phone)) {
            showToast("手机号格式错误")
            return false
        }

        if (!InputValidator.isPasswordValid(password)) {
            showToast("密码格式错误（5-16位非空字符）")
            return false
        }

        if (password != confirmPassword) {
            showToast("两次输入的密码不一致")
            return false
        }

        if (verificationCode.isBlank()) {
            showToast("请输入验证码")
            return false
        }

        return true
    }

    private fun handleRegisterResult(apiresponse: ApiResponse<Unit>) {
        if (apiresponse.code == "00000") {
            showToast("注册成功")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            showToast(apiresponse.msg ?: "注册失败：${apiresponse.code}")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}