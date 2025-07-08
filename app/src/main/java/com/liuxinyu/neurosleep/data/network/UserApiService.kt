package com.liuxinyu.neurosleep.data.network

import com.liuxinyu.neurosleep.feature.home.experiment.Experimentvo
import com.liuxinyu.neurosleep.core.auth.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface UserApiService {
    @POST("/user/register")
    @FormUrlEncoded
    suspend fun register(
        @Field("phone") phone: String,
        @Field("password") password: String,
        @Field("id") id: Int = 1  // 默认值为1
    ): ApiResponse<Unit>

    @FormUrlEncoded
    @POST("user/login")
    suspend fun login(
        @Field("phone") phone: String,
        @Field("password") password: String,
        @Field("id") id: Int = 1  // 默认值为1
    ): ApiResponse<LoginResponse>

    @GET("/user/experiments")
    suspend fun getExperiments(
        @Header("Authorization") token: String
    ): ApiResponse<List<Experimentvo>>

    @Multipart
    @POST("/files/chunk")  // 与后端 @PostMapping("/chunk") 匹配
    suspend fun uploadChunk(
        @Part file: MultipartBody.Part,
        @Part("experimentId") experimentId: RequestBody?,  // 允许为空
        @Part("chunkIndex") chunkIndex: RequestBody,
        @Part("totalChunks") totalChunks: RequestBody,
        @Part("SNCode") snCode: RequestBody,  // 参数名与后端完全一致
        @Part labelDTO: MultipartBody.Part?,   // 以 Part 形式传递 JSON
        @Header("Authorization") token: String // 统一认证
    ): ApiResponse<String>  // 统一业务响应

    @POST("experiment/join")
    suspend fun joinExperiment(
        @Query("inviteCode") inviteCode: String,
        @Header("Authorization") token: String
    ): Response<Unit>
}

// 通用响应模型
data class ApiResponse<T>(
    val code: Int,
    val msg: String?,
    val data: T?
)