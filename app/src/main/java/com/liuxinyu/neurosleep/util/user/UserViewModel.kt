package com.liuxinyu.neurosleep.util.user

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liuxinyu.neurosleep.core.auth.LoginResponse
import com.liuxinyu.neurosleep.data.network.ApiResponse
import com.liuxinyu.neurosleep.data.network.LoginRequest
import com.liuxinyu.neurosleep.data.network.RegisterRequest
import com.liuxinyu.neurosleep.data.network.RetrofitClient
import com.liuxinyu.neurosleep.data.network.UserApiService
import com.liuxinyu.neurosleep.feature.home.experiment.Experimentvo

class UserViewModel(private val context: Context) : ViewModel() {
    private val apiService = RetrofitClient.getInstance(context).create(UserApiService::class.java)

    // 注册逻辑
    suspend fun register(phone: String, password: String): ApiResponse<Unit> {
        return try {
            val request = RegisterRequest(phone, password)
            val response = apiService.register(request)
            if (response.code == "00000") {  // 修改为检查 code == 0
                response // 直接返回正确的响应
            } else {
                ApiResponse(response.code, response.msg ?: "Unknown error", null)
            }
        } catch (e: Exception) {
            ApiResponse("A0400", "Network error: ${e.message}", null)
        }
    }

    // 登录逻辑
    /*suspend fun login(phone: String, password: String): ApiResponse<Map<String, String>> {
        return try {
            val response = apiService.login(phone, password)
            if (response.code == 200) {
                response
            } else {
                ApiResponse(response.code, response.msg ?: "Unknown error", null)
            }
        } catch (e: Exception) {
            ApiResponse(-1, "Network error: ${e.message}", null)
        }
    }*/
    suspend fun login(phone: String, password: String): ApiResponse<LoginResponse> {
        return try {
            val request = LoginRequest(phone, password)
            val response = apiService.login(request)
            if (response.code == "00000") {  // 服务器返回 code 0 表示成功
                val loginData = response.data
                if (loginData != null) {
                    ApiResponse("00000","操作成功", loginData)  // 使用服务器返回的成功消息
                } else {
                    ApiResponse("A0400", "登录失败：Token 缺失", null)
                }
            } else {
                ApiResponse(response.code, response.msg ?: "登录失败", null)
            }
        } catch (e: Exception) {
            ApiResponse("A0400", "网络错误: ${e.message}", null)
        }
    }

    // 获取实验列表
    suspend fun getExperiments(token: String): ApiResponse<List<Experimentvo>> {
        return try {
            // 确保token已经包含Bearer前缀，如果没有则添加
            val formattedToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = apiService.getExperiments(formattedToken)
            if (response.code == "00000") {  // 修改为检查 code == 0
                response
            } else {
                ApiResponse(response.code, response.msg ?: "Unknown error", null)
            }
        } catch (e: Exception) {
            ApiResponse("A0400", "Network error: ${e.message}", null)
        }
    }
}

class UserViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
