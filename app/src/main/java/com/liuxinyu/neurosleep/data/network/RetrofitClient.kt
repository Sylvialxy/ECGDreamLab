package com.liuxinyu.neurosleep.data.network

import android.content.Context
import com.liuxinyu.neurosleep.util.user.AuthManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.105.0.33:20000"

    private val interceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 创建一个函数来获取带有 context 的 Retrofit 实例
    fun getInstance(context: Context): Retrofit {
        // 不再自动添加 Authorization header，让每个 API 调用手动传递
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 保留原有的 instance 属性以保持向后兼容，但不推荐使用
    @Deprecated("Use getInstance(context) instead", ReplaceWith("getInstance(context)"))
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}