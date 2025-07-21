package com.liuxinyu.neurosleep.data.network

import com.liuxinyu.neurosleep.feature.home.experiment.Experimentvo
import com.liuxinyu.neurosleep.core.auth.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url

interface UserApiService {
    @POST("/api/user/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): ApiResponse<Unit>

    @POST("/api/user/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginResponse>

    @GET("/user/experiments")
    suspend fun getExperiments(
        @Header("Authorization") token: String
    ): ApiResponse<List<Experimentvo>>

    // 获取预签名上传URL
    @POST("/api/files/presigned-upload")
    suspend fun getPresignedUploadUrls(
        @Body request: PresignedUploadRequest,
        @Header("Authorization") token: String
    ): ApiResponse<PresignedUploadResponse>

    // 使用预签名URL上传分片
    @PUT
    suspend fun uploadChunkToPresignedUrl(
        @Url presignedUrl: String,
        @Body chunkData: RequestBody
    ): Response<ResponseBody>

    // 完成多部分上传
    @POST("/api/files/complete-multipart-upload")
    suspend fun completeMultipartUpload(
        @Body request: CompleteMultipartUploadRequest,
        @Header("Authorization") token: String
    ): ApiResponse<String>
    
    // 文件上传回调接口
    @POST("/api/files/presigned-upload-recall")
    suspend fun fileUploadRecall(
        @Body request: FileUploadRecallRequest,
        @Header("Authorization") token: String
    ): ApiResponse<Unit>

    @POST("experiment/join")
    suspend fun joinExperiment(
        @Query("inviteCode") inviteCode: String,
        @Header("Authorization") token: String
    ): Response<Unit>
}

// 通用响应模型
data class ApiResponse<T>(
    val code: String,
    val msg: String?,
    val data: T?
)

// 请求数据模型
data class LoginRequest(
    val phone: String,
    val password: String
)

data class RegisterRequest(
    val phone: String,
    val password: String
)

// 预签名上传请求新格式
data class PresignedUploadRequest(
    val files: List<FileUploadRequest>
)

data class FileUploadRequest(
    val originalFilename: String,
    val contentType: String = "application/bin", // 默认值
    val partCount: Int,
    val SNCode: String,
    // 保留额外字段以备用
    val experimentId: Int? = null,
    val labelDTO: String? = null // JSON字符串形式的标签数据
)

// 旧版请求类改名保留兼容
@Deprecated("使用新版PresignedUploadRequest和FileUploadRequest类")
data class OldPresignedUploadRequest(
    val originalFilename: String,
    val fileSize: Long,
    val experimentId: Int?,
    val snCode: String,
    val labelDTO: String? = null // JSON字符串形式的标签数据
)

// 预签名上传响应
data class PresignedUploadResponse(
    val filesUploadMeta: List<FileUploadMeta>
)

data class FileUploadMeta(
    val originalFilename: String,
    val objectName: String,
    val presignedUrl: String?,
    val expiryTime: Long,
    val uploadId: String,
    val chunkPresignedUrls: List<ChunkPresignedUrl>
)

data class ChunkPresignedUrl(
    val partNumber: Int,
    val presignedUrl: String
)

// 完成多部分上传请求
data class CompleteMultipartUploadRequest(
    val objectName: String,
    val uploadId: String,
    val parts: List<UploadPart>
)

data class UploadPart(
    val partNumber: Int,
    val etag: String
)

// 文件上传回调请求
data class FileUploadRecallRequest(
    val recallList: List<FileUploadRecallItem>
)

data class FileUploadRecallItem(
    val objectName: String,
    val contentType: String = ""
)