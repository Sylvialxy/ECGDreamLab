package com.liuxinyu.neurosleep.feature.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.core.main.FlashPageActivity
import com.liuxinyu.neurosleep.data.database.AppDatabase
import com.liuxinyu.neurosleep.data.model.EcgLabel
import com.liuxinyu.neurosleep.data.model.EcgLabelDTO
import com.liuxinyu.neurosleep.data.network.RetrofitClient
import com.liuxinyu.neurosleep.data.network.UserApiService
import com.liuxinyu.neurosleep.data.network.PresignedUploadRequest
import com.liuxinyu.neurosleep.data.network.CompleteMultipartUploadRequest
import com.liuxinyu.neurosleep.data.network.UploadPart
import com.liuxinyu.neurosleep.data.network.FileUploadRequest
import com.liuxinyu.neurosleep.data.network.CreateLabelsRequest
import com.liuxinyu.neurosleep.data.network.StatusLabel
import com.liuxinyu.neurosleep.data.repository.EcgLabelRepository
import com.liuxinyu.neurosleep.databinding.FragmentHomeBinding
import com.liuxinyu.neurosleep.feature.home.experiment.JoinExperimentFragment
import com.liuxinyu.neurosleep.feature.home.label.MakeLabelFragment
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModel
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModelFactory
import com.liuxinyu.neurosleep.util.user.AuthManager
import com.liuxinyu.neurosleep.core.ble.ByteUtil
import com.liuxinyu.neurosleep.util.FormattedTime
import com.yabu.livechart.model.DataPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.LinkedList
import java.util.Timer

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EcgLabelViewModel
    // 数据可视化相关
    private val slidingWindow = LinkedList<DataPoint>()
    private val dataList = mutableListOf<DataPoint>()
    private val slidingWindowSize = 20

    // 定时任务控制
    private var timer: Timer? = null

    companion object {
        const val TAG = "HomeFragment"
    }

    /**
     * 处理 token 过期的情况
     */
    private fun handleTokenExpired() {
        Toast.makeText(requireContext(), "登录已过期，请重新登录", Toast.LENGTH_LONG).show()
        // 清除过期的 token
        AuthManager.clearToken(requireContext())

        // 跳转到登录页面
        val intent = Intent(requireContext(), com.liuxinyu.neurosleep.core.auth.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    // DrawerLayout 控件
    private lateinit var drawerLayout: DrawerLayout

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            Log.i(TAG, "File selected: $uri")
            uploadFile(it) // 直接使用 Uri 上传文件
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 使用ViewBinding绑定视图
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // 初始化依赖
        val dao = AppDatabase.getInstance(requireContext()).ecgLabelDao()
        val repository = EcgLabelRepository.getInstance(dao, requireContext())
        
        // 设置当前用户ID
        val phone = AuthManager.getPhone(requireContext())
        if (phone == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return binding.root
        }
        repository.setUserId(phone)
        
        val factory = EcgLabelViewModelFactory(repository, requireContext())
        viewModel = ViewModelProvider(requireActivity(), factory)[EcgLabelViewModel::class.java]

        return binding.root
    }

    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 观察标签数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.labels.collect { labels ->
                    if (labels.isNotEmpty()) {
                        // 启用上传按钮等UI状态
                        Log.d(TAG, "Received labels: ${labels.size}")
                    }
                }
            }
        }

        view.findViewById<Button>(R.id.btn_bluetooth_data).setOnClickListener {
            Log.d(TAG, "Bluetooth button clicked") // 确认点击事件触发
            
            // 检查用户是否已登录
            val phone = AuthManager.getPhone(requireContext())
            if (phone == null) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 启动新的Activity
            val intent = Intent(requireContext(), BluetoothDataActivity::class.java)
            startActivity(intent)
        }

        // 获取 DrawerLayout 和侧边栏
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)
        // 从 drawerLayout 中获取侧边栏
        val sideMenu = drawerLayout.findViewById<LinearLayout>(R.id.side_menu)

        // 设置用户图标点击事件，打开侧边栏
        binding.userIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)  // 打开侧边栏
        }

        // 设置菜单项点击事件
        sideMenu.findViewById<TextView>(R.id.menu_option_1).setOnClickListener {
            // 处理主功能设置的点击事件
            Log.d(TAG, "主功能设置 clicked")
            
            // 跳转到加入实验页面
            val joinExperimentFragment = JoinExperimentFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, joinExperimentFragment)
                .addToBackStack(null)
                .commit()
        }

        sideMenu.findViewById<TextView>(R.id.menu_option_2).setOnClickListener {
            // 处理切换用户的点击事件
            Log.d(TAG, "切换用户 clicked")

            // 清除记住的手机号和密码
            val prefs = requireActivity().getSharedPreferences("login_prefs", MODE_PRIVATE)
            with(prefs.edit()) {
                remove("saved_phone")  // 移除保存的手机号
                remove("saved_password")  // 移除保存的密码
                apply()
            }

            // 清除保存的Token和用户信息
            AuthManager.clearToken(requireContext()) // 你需要在 AuthManager 中实现这个方法

            // 跳转到 FlashPageActivity12
            val intent = Intent(requireContext(), FlashPageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // 清空当前活动栈
            startActivity(intent)
            requireActivity().finish() // 关闭当前活动
        }

        sideMenu.findViewById<TextView>(R.id.menu_option_3).setOnClickListener {
            // 处理VIP服务的点击事件
            Log.d(TAG, "VIP服务 clicked")
        }

        sideMenu.findViewById<TextView>(R.id.menu_option_4).setOnClickListener {
            // 处理智能诊断的点击事件
            Log.d(TAG, "智能诊断 clicked")
        }

        // 设置上传按钮点击事件
        binding.uploadBtn.setOnClickListener {
            // 点击上传按钮时，弹出文件选择器
            openDocument.launch(arrayOf("*/*")) // 可以根据需要修改文件类型，比如支持所有文件或特定类型
        }

        // 添加打标签按钮点击事件
        binding.makelabelBtn.setOnClickListener {
            // 检查用户是否已登录
            val phone = AuthManager.getPhone(requireContext())
            if (phone == null) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 替换当前Fragment为MakeLabelFragment
            val makeLabelFragment = MakeLabelFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, makeLabelFragment)
                .addToBackStack(null)
                .commit()
        }

        // 添加加入实验按钮点击事件
        binding.joinExperimentBtn.setOnClickListener {
            // 检查用户是否已登录
            val phone = AuthManager.getPhone(requireContext())
            if (phone == null) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 跳转到加入实验页面
            val joinExperimentFragment = JoinExperimentFragment()
            parentFragmentManager.beginTransaction()
                .add(R.id.fragment_container, joinExperimentFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun uploadFile(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val fileSize = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: return
        val fileName = getFileName(uri)

        val bufferSize = 5 * 1024 * 1024 // 每块 5MB
        val totalChunks = ((fileSize + bufferSize - 1) / bufferSize).toInt()

        // 从SharedPreferences获取真实的SNCode
        val sharedPref = requireContext().getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        val snCode = sharedPref.getString("SN_CODE", null) ?: run {
            Log.e(TAG, "SNCode not found, please connect to device first")
            Toast.makeText(requireContext(), "请先连接设备", Toast.LENGTH_SHORT).show()
            return
        }

        val token = AuthManager.getFormattedToken(requireContext())
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token is null or empty")
            Toast.makeText(requireContext(), "请重新登录", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Token retrieved: ${token.take(15)}...")

        // 验证 token 格式（简单检查）
        if (token.length < 10) {
            Log.e(TAG, "Token seems too short, might be invalid")
            Toast.makeText(requireContext(), "Token 格式异常，请重新登录", Toast.LENGTH_SHORT).show()
            return
        }

        // 直接检查标签并开始上传，如果 token 无效会在上传时处理
        Log.d(TAG, "Starting upload process...")
        Log.d(TAG, "Device SN: $snCode")

        // 从 SharedPreferences 获取当前选中的实验 ID 和名称（用于 presigned-upload 接口）
        val experimentPrefs = requireContext().getSharedPreferences("EXPERIMENT_PREFS", Context.MODE_PRIVATE)
        val studyId = experimentPrefs.getInt("SELECTED_EXPERIMENT_ID", -1)
        val studyName = experimentPrefs.getString("SELECTED_EXPERIMENT_NAME", null)

        if (studyId == -1) {
            Toast.makeText(requireContext(), "请先选择实验\n点击实验列表中的实验进行选择", Toast.LENGTH_LONG).show()
            // 跳转到加入实验页面
            val joinExperimentFragment = JoinExperimentFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, joinExperimentFragment)
                .addToBackStack(null)
                .commit()
            return
        }

        Log.d(TAG, "Using study ID: $studyId, name: $studyName")

        val currentLabels = viewModel.labels.value
        Log.d(TAG, "Current labels size: ${currentLabels.size}")

        if (currentLabels.isEmpty()) {
            Log.e(TAG, "No valid label data found")
            Toast.makeText(requireContext(), "无有效标签数据", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查是否有未完成的标签
        val unfinishedLabel = currentLabels.find { it.endTime == null }

        if (unfinishedLabel != null) {
            // 显示确认对话框
            AlertDialog.Builder(requireContext())
                .setTitle("有未完成的标签")
                .setMessage("当前有正在进行的标签，是否结束并上传？")
                .setPositiveButton("确定") { _, _ ->
                    // 结束未完成的标签
                    unfinishedLabel.endTime = LocalDateTime.now()
                    viewModel.updateLabel(unfinishedLabel)
                    // 显示实验确认对话框
                    showStudyConfirmationDialog(uri, inputStream, fileSize, fileName, bufferSize, totalChunks, snCode, token, studyId, studyName)
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            // 没有未完成的标签，显示实验确认对话框
            showStudyConfirmationDialog(uri, inputStream, fileSize, fileName, bufferSize, totalChunks, snCode, token, studyId, studyName)
        }
    }

    /**
     * 显示实验确认对话框
     */
    private fun showStudyConfirmationDialog(
        uri: Uri,
        inputStream: java.io.InputStream,
        fileSize: Long,
        fileName: String?,
        bufferSize: Int,
        totalChunks: Int,
        snCode: String,
        token: String,
        studyId: Int,
        studyName: String?
    ) {
        val message = if (studyName != null) {
            "确认上传数据到以下实验？\n\n实验名称：$studyName\n实验ID：$studyId"
        } else {
            "确认上传数据到实验 ID：$studyId？"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("确认上传")
            .setMessage(message)
            .setPositiveButton("确定上传") { _, _ ->
                // 继续上传流程
                proceedWithUpload(uri, inputStream, fileSize, fileName, bufferSize, totalChunks, snCode, token, studyId)
            }
            .setNegativeButton("取消", null)
            .show()
    }



    private fun proceedWithUpload(
        uri: Uri,
        inputStream: java.io.InputStream,
        fileSize: Long,
        fileName: String?,
        bufferSize: Int,
        totalChunks: Int,
        snCode: String,
        token: String,
        studyId: Int?
    ) {
        val currentLabels = viewModel.labels.value
        val labelDTO = EcgLabelDTO(
            labels = currentLabels,
            checksum = calculateChecksum(currentLabels)
        )
        Log.d(TAG, "Created labelDTO with ${currentLabels.size} labels")

        // 添加序列化数据的日志
        val json = Gson().toJson(labelDTO)
        Log.d(TAG, "Serialized labelDTO: $json")

        val retrofit = RetrofitClient.getInstance(requireContext()).create(UserApiService::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 确保token格式正确
                val formattedToken = if (!token.startsWith("Bearer ")) "Bearer $token" else token

                // 第一步：先创建标签记录
                Log.d(TAG, "Step 1: Creating label records")

                // 将 EcgLabel 转换为后端期望的 StatusLabel 格式
                // 后端要求格式：YYYY-MM-DDTHH:mm:ss.SSS（包含毫秒）
                val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

                // 从ECG.BIN文件读取采集开始时间
                val collectionStartTime = getCollectionStartTime(uri)
                val earliestStartTime = currentLabels.minByOrNull { it.startTime }?.startTime

                // 解析collectionStartTime为LocalDateTime用于计算采样点
                val collectionStartDateTime = try {
                    LocalDateTime.parse(collectionStartTime, timeFormatter)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse collectionStartTime: $collectionStartTime", e)
                    LocalDateTime.now()
                }

                val statusLabels = currentLabels.mapNotNull { label ->
                    label.endTime?.let { endTime ->
                        // 从 customName 中分离一级标签和二级标签
                        // customName 格式为 "一级标签-二级标签"，例如 "睡眠-无干预"
                        val (primaryLabel, secondaryLabel) = if (label.customName?.contains("-") == true) {
                            val parts = label.customName.split("-", limit = 2)
                            Pair(parts[0], parts.getOrNull(1) ?: "")
                        } else {
                            // 如果没有"-"分隔符，使用默认值
                            Pair(label.customName ?: "未知", "")
                        }

                        // 计算采样点：采样率为200Hz
                        val samplingRate = 200L // 每秒200个采样点

                        // 计算事件开始时间相对于采集开始时间的秒数差
                        val startDuration = java.time.Duration.between(collectionStartDateTime, label.startTime)
                        val startSeconds = startDuration.seconds
                        val startSamplePoint = startSeconds * samplingRate

                        // 计算事件结束时间相对于采集开始时间的秒数差
                        val endDuration = java.time.Duration.between(collectionStartDateTime, endTime)
                        val endSeconds = endDuration.seconds
                        val endSamplePoint = endSeconds * samplingRate

                        Log.d(TAG, "Label: $primaryLabel-$secondaryLabel, startSample: $startSamplePoint, endSample: $endSamplePoint")

                        StatusLabel(
                            type = primaryLabel,  // 一级标签（如"睡眠"）
                            recordingStartTime = earliestStartTime?.format(timeFormatter) ?: collectionStartTime,
                            status = secondaryLabel,  // 二级标签（如"无干预"）
                            eventStartTime = label.startTime.format(timeFormatter),
                            eventEndTime = endTime.format(timeFormatter),
                            startSamplePoint = startSamplePoint,
                            endSamplePoint = endSamplePoint
                        )
                    }
                }

                if (statusLabels.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "没有完成的标签可以上传", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val createLabelsRequest = CreateLabelsRequest(
                    deviceSn = snCode,
                    studyId = studyId ?: 1,  // 使用选中的实验 ID，如果为 null 则使用默认值 1
                    collectionStartTime = collectionStartTime,
                    statusLabels = statusLabels
                )

                // 添加调试日志，查看请求内容
                val requestJson = Gson().toJson(createLabelsRequest)
                Log.d(TAG, "Creating labels with ${statusLabels.size} status labels")
                Log.d(TAG, "Device SN: $snCode, Study ID: ${studyId ?: 1}")
                Log.d(TAG, "Collection Start Time: $collectionStartTime")
                Log.d(TAG, "Create Labels Request JSON: $requestJson")
                val createLabelsResponse = retrofit.createLabels(createLabelsRequest, formattedToken)
                Log.d(TAG, "Create labels response code: ${createLabelsResponse.code}, msg: ${createLabelsResponse.msg}")

                if (createLabelsResponse.code == "00000") {
                    val fileId = createLabelsResponse.data?.fileId
                    Log.d(TAG, "Labels created successfully, fileId: $fileId")

                    // 保存 fileId 到 SharedPreferences，后续可能需要使用
                    if (fileId != null) {
                        val uploadPrefs = requireContext().getSharedPreferences("UPLOAD_PREFS", Context.MODE_PRIVATE)
                        uploadPrefs.edit().putInt("LAST_FILE_ID", fileId).apply()
                        Log.d(TAG, "Saved fileId: $fileId to SharedPreferences")
                    }
                } else {
                    Log.e(TAG, "Failed to create labels: ${createLabelsResponse.msg}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "创建标签失败: ${createLabelsResponse.msg}", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // 第二步：获取预签名上传URL
                Log.d(TAG, "Step 2: Getting presigned upload URLs")

                // 使用新的API请求格式
                val fileRequest = FileUploadRequest(
                    originalFilename = fileName ?: "unknown.bin",
                    contentType = "application/bin",
                    partCount = totalChunks,
                    deviceSn = snCode,  // 使用 deviceSn 与创建标签请求保持一致
                    studyId = studyId,  // 使用 studyId 字段
                    collectionStartTime = collectionStartTime  // 添加收集开始时间
                )

                val presignedRequest = PresignedUploadRequest(
                    files = listOf(fileRequest)
                )

                // 添加调试日志
                val presignedRequestJson = Gson().toJson(presignedRequest)
                Log.d(TAG, "Presigned Upload Request JSON: $presignedRequestJson")

                val presignedResponse = retrofit.getPresignedUploadUrls(presignedRequest, formattedToken)

                Log.d(TAG, "Presigned response code: ${presignedResponse.code}, msg: ${presignedResponse.msg}")

                if (presignedResponse.code != "00000" || presignedResponse.data == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "获取上传URL失败: ${presignedResponse.msg}", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val uploadMeta = presignedResponse.data.filesUploadMeta.firstOrNull()
                if (uploadMeta == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "上传元数据为空", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                Log.d(TAG, "Got upload meta: uploadId=${uploadMeta.uploadId}, chunks=${uploadMeta.chunkPresignedUrls.size}")

                // 第三步：使用预签名URL上传各个分片
                Log.d(TAG, "Step 3: Uploading chunks using presigned URLs")
                val uploadedParts = mutableListOf<UploadPart>()
                var uploadSuccess = true

                inputStream.use { stream ->
                    val buffer = ByteArray(bufferSize)
                    var bytesRead: Int
                    var chunkIndex = 0
                    var totalBytesUploaded = 0L

                    for (chunkUrl in uploadMeta.chunkPresignedUrls) {
                        bytesRead = stream.read(buffer)
                        if (bytesRead <= 0) break

                        val chunkBytes = buffer.copyOf(bytesRead)
                        totalBytesUploaded += bytesRead

                        // 计算上传进度
                        val progress = (totalBytesUploaded * 100 / fileSize).toInt()
                        Log.d(TAG, "Uploading chunk ${chunkUrl.partNumber} of ${uploadMeta.chunkPresignedUrls.size} (${progress}%)")

                        try {
                            val chunkBody = chunkBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                            val response = retrofit.uploadChunkToPresignedUrl(chunkUrl.presignedUrl, chunkBody)

                            if (response.isSuccessful) {
                                // 获取ETag
                                val etag = response.headers()["ETag"]?.removeSurrounding("\"") ?: ""
                                uploadedParts.add(UploadPart(chunkUrl.partNumber, etag))

                                withContext(Dispatchers.Main) {
                                    Log.d(TAG, "Chunk ${chunkUrl.partNumber} uploaded successfully, ETag: $etag")
                                    val progressText = "上传进度: $progress%"
                                    Toast.makeText(requireContext(), progressText, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Log.e(TAG, "Chunk ${chunkUrl.partNumber} upload failed: ${response.code()}")
                                uploadSuccess = false
                                break
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Log.e(TAG, "Chunk ${chunkUrl.partNumber} upload failed", e)
                                if (e.message?.contains("401") == true) {
                                    handleTokenExpired()
                                } else {
                                    Toast.makeText(requireContext(), "分块 ${chunkUrl.partNumber} 上传失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            uploadSuccess = false
                            break
                        }

                        chunkIndex++
                    }
                }

                // 第四步：完成多部分上传
                if (uploadSuccess && uploadedParts.isNotEmpty()) {
                    completeMultipartUpload(uploadMeta.objectName, uploadMeta.uploadId, uploadedParts, formattedToken, retrofit)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Upload failed", e)
                    Toast.makeText(requireContext(), "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 完成多部分上传，将已上传的分片合并为完整文件
     * @param objectName 对象名称
     * @param uploadId 上传ID
     * @param parts 已上传的分片列表
     * @param token 授权令牌
     * @param retrofit Retrofit客户端
     */
    private suspend fun completeMultipartUpload(
        objectName: String,
        uploadId: String,
        parts: List<UploadPart>,
        token: String,
        retrofit: UserApiService
    ) {
        try {
            Log.d(TAG, "Step 4: Completing multipart upload")
            Log.d(TAG, "Object name: $objectName")
            Log.d(TAG, "Upload ID: $uploadId")
            Log.d(TAG, "Parts count: ${parts.size}")
            
            // 构建请求体
            val completeRequest = CompleteMultipartUploadRequest(
                objectName = objectName,
                uploadId = uploadId,
                parts = parts
            )

            // 记录请求详情
            val gson = Gson()
            Log.d(TAG, "Complete request: ${gson.toJson(completeRequest)}")

            // 调用API
            val completeResponse = retrofit.completeMultipartUpload(completeRequest, token)
            
            // 处理响应
            if (completeResponse.code == "00000") {
                viewModel.clearSession()
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "File upload completed successfully, response: ${completeResponse.data}")
                    Toast.makeText(requireContext(), "上传完成", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Complete upload failed with code: ${completeResponse.code}, message: ${completeResponse.msg}")
                    Toast.makeText(requireContext(), "完成上传失败: ${completeResponse.msg}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e(TAG, "Complete multipart upload failed with exception", e)
                Toast.makeText(requireContext(), "文件合并失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                // 尝试获取 DISPLAY_NAME
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return it.getString(displayNameIndex)
                }
            }
        }
        // 如果无法获取 DISPLAY_NAME，则从 Uri 的路径中提取文件名
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun calculateChecksum(labels: List<EcgLabel>): String {
        // 1. 准备时间格式化器（确保时间格式一致）
        val timeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        // 2. 构建可排序的标签数据字符串
        val sortedLabels = labels.sortedBy { it.startTime } // 按开始时间排序确保顺序一致
        val labelStrings = sortedLabels.map { label ->
            // 3. 标准化每个标签的数据表示
            buildString {
                append(label.labelType.name)           // 标签类型（枚举名）
                append("|")
                append(label.startTime.format(timeFormatter)) // 标准化开始时间
                append("|")
                append(label.endTime?.format(timeFormatter) ?: "null") // 处理可空结束时间
                append("|")
                append(label.customName ?: "")         // 处理可空自定义名称
            }
        }

        // 4. 连接所有标签数据
        val combinedData = labelStrings.joinToString("#")

        // 5. 创建 SHA-256 摘要实例
        val digest = MessageDigest.getInstance("SHA-256")

        // 6. 计算哈希值
        val hashBytes = digest.digest(combinedData.toByteArray(Charsets.UTF_8))

        // 7. 转换为十六进制字符串
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 从ECG.BIN文件读取采集开始时间
     *
     * @param uri ECG.BIN文件的URI
     * @return 格式化的采集开始时间字符串（YYYY-MM-DDTHH:mm:ss.SSS），如果读取失败则使用最早标签时间或当前时间
     */
    private fun getCollectionStartTime(uri: Uri): String {
        val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

        try {
            // 尝试从ECG.BIN文件头读取采集开始时间
            val contentResolver = requireContext().contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                // 读取文件头部的前32字节
                val headerBytes = ByteArray(32)
                val bytesRead = inputStream.read(headerBytes)

                if (bytesRead >= 12) {
                    // 解析ECG.BIN文件头
                    val formattedTime = ByteUtil.parseEcgBinHeader(headerBytes)

                    if (formattedTime != null) {
                        // 将FormattedTime转换为LocalDateTime
                        val collectionStartDateTime = LocalDateTime.of(
                            formattedTime.year,
                            formattedTime.month,
                            formattedTime.day,
                            formattedTime.hour,
                            formattedTime.minute,
                            formattedTime.second
                        )

                        val result = collectionStartDateTime.format(timeFormatter)
                        Log.d(TAG, "Collection start time from ECG.BIN: $result")
                        return result
                    } else {
                        Log.w(TAG, "Failed to parse ECG.BIN header, falling back to label time")
                    }
                } else {
                    Log.w(TAG, "ECG.BIN file too short (${bytesRead} bytes), falling back to label time")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading ECG.BIN header", e)
        }

        // 如果从文件读取失败，回退到使用最早的标签开始时间
        val currentLabels = viewModel.labels.value
        val earliestTime = currentLabels.minByOrNull { it.startTime }?.startTime

        val fallbackTime = earliestTime?.format(timeFormatter) ?: LocalDateTime.now().format(timeFormatter)
        Log.d(TAG, "Using fallback collection start time: $fallbackTime")
        return fallbackTime
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }
}
