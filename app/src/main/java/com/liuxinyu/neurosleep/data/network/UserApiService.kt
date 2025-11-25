package com.liuxinyu.neurosleep.data.network

import com.google.gson.annotations.SerializedName
import com.liuxinyu.neurosleep.feature.home.experiment.StudyVO
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

    // 创建标签记录（在上传文件之前调用）
    @POST("/api/files/status-label")
    suspend fun createLabels(
        @Body request: CreateLabelsRequest,
        @Header("Authorization") token: String
    ): ApiResponse<CreateLabelsResponse>

    // 获取已加入的实验列表
    @GET("/api/study/list")
    suspend fun getJoinedStudies(
        @Header("Authorization") token: String
    ): ApiResponse<List<StudyVO>>

    // 获取睡眠阶段预测数据
    @POST("/api/algorithm/sleep-stage-app")
    suspend fun getSleepStagePredictions(
        @Body request: SleepStageRequest,
        @Header("Authorization") token: String
    ): ApiResponse<SleepStageResponse>
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
    @SerializedName("originalFilename")
    val originalFilename: String,

    @SerializedName("contentType")
    val contentType: String = "application/bin", // 默认值

    @SerializedName("partCount")
    val partCount: Int,

    @SerializedName("deviceSn")  // 使用 deviceSn 与创建标签请求保持一致
    val deviceSn: String,

    @SerializedName("studyId")  // 后端使用 studyId
    val studyId: Int? = null,

    @SerializedName("collectionStartTime")  // 收集开始时间
    val collectionStartTime: String? = null
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

// 创建标签请求 - 匹配后端 /api/files/status-label 的实际要求
// 注意：虽然文档中没有，但后端实际需要 studyId 字段
data class CreateLabelsRequest(
    @SerializedName("deviceSn")
    val deviceSn: String,                    // 设备 SN

    @SerializedName("studyId")
    val studyId: Int,                        // 实验 ID（后端必需）

    @SerializedName("collectionStartTime")
    val collectionStartTime: String,         // 收集开始时间

    @SerializedName("statusLabels")
    val statusLabels: List<StatusLabel>      // 标签值列表
)

// 状态标签
data class StatusLabel(
    @SerializedName("type")
    val type: String,                        // 一级标签（如"睡觉"）

    @SerializedName("recordingStartTime")
    val recordingStartTime: String,          // 记录开始时间

    @SerializedName("status")
    val status: String,                      // 二级标签（如"无干预"）

    @SerializedName("eventStartTime")
    val eventStartTime: String,              // 事件开始时间

    @SerializedName("eventEndTime")
    val eventEndTime: String,                // 事件结束时间

    @SerializedName("startSamplePoint")
    val startSamplePoint: Long = 0,          // 开始采样点（默认0）

    @SerializedName("endSamplePoint")
    val endSamplePoint: Long = 0             // 结束采样点（默认0）
)

// 创建标签响应
data class CreateLabelsResponse(
    @SerializedName("fileId")
    val fileId: Int? = null,    // 文件ID（后端返回）
    val labelId: Long? = null,  // 创建的标签ID（保留兼容）
    val message: String? = null
)

// 睡眠阶段预测请求
data class SleepStageRequest(
    @SerializedName("fileId")
    val fileId: Int  // 文件ID
)

// 睡眠阶段预测响应
data class SleepStageResponse(
    @SerializedName("predictions")
    val predictions: List<Int>,  // 预测结果数组：0=Wake, 1=N1, 2=N2, 3=N3, 4=REM

    @SerializedName("collectionStartTime")
    val collectionStartTime: String  // 采集开始时间，格式："2025-02-13T15:53:50"
)