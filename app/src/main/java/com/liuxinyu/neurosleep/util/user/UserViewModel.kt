package com.liuxinyu.neurosleep.util.user

import androidx.lifecycle.ViewModel
import com.liuxinyu.neurosleep.core.auth.LoginResponse
import com.liuxinyu.neurosleep.data.network.ApiResponse
import com.liuxinyu.neurosleep.data.network.RetrofitClient
import com.liuxinyu.neurosleep.data.network.UserApiService
import com.liuxinyu.neurosleep.feature.home.experiment.Experimentvo

class UserViewModel : ViewModel() {
    private val apiService = RetrofitClient.instance.create(UserApiService::class.java)

    // 注册逻辑
    suspend fun register(phone: String, password: String): ApiResponse<Unit> {
        return try {
            val response = apiService.register(phone, password, 1)
            if (response.code == 0) {  // 修改为检查 code == 0
                response // 直接返回正确的响应
            } else {
                ApiResponse(response.code, response.msg ?: "Unknown error", null)
            }
        } catch (e: Exception) {
            ApiResponse(-1, "Network error: ${e.message}", null)
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
            val response = apiService.login(phone, password, 1)
            if (response.code == 0) {  // 服务器返回 code 0 表示成功
                val loginData = response.data
                if (loginData != null) {
                    ApiResponse(0, "操作成功", loginData)  // 使用服务器返回的成功消息
                } else {
                    ApiResponse(-1, "登录失败：Token 缺失", null)
                }
            } else {
                ApiResponse(response.code, response.msg ?: "登录失败", null)
            }
        } catch (e: Exception) {
            ApiResponse(-1, "网络错误: ${e.message}", null)
        }
    }

    // 获取实验列表
    suspend fun getExperiments(token: String): ApiResponse<List<Experimentvo>> {
        return try {
            val response = apiService.getExperiments("Bearer $token")
            if (response.code == 0) {  // 修改为检查 code == 0
                response
            } else {
                ApiResponse(response.code, response.msg ?: "Unknown error", null)
            }
        } catch (e: Exception) {
            ApiResponse(-1, "Network error: ${e.message}", null)
        }
    }
}
