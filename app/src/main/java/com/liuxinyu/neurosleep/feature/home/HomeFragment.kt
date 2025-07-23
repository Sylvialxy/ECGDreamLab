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
import com.liuxinyu.neurosleep.data.model.StatusLabelRequest
import com.liuxinyu.neurosleep.data.model.StatusLabelConverter
import com.liuxinyu.neurosleep.data.network.RetrofitClient
import com.liuxinyu.neurosleep.data.network.UserApiService
import com.liuxinyu.neurosleep.data.network.PresignedUploadRequest
import com.liuxinyu.neurosleep.data.network.CompleteMultipartUploadRequest
import com.liuxinyu.neurosleep.data.network.UploadPart
import com.liuxinyu.neurosleep.data.network.FileUploadRequest
import com.liuxinyu.neurosleep.data.network.ApiResponse
import com.liuxinyu.neurosleep.data.repository.EcgLabelRepository
import com.liuxinyu.neurosleep.databinding.FragmentHomeBinding
import com.liuxinyu.neurosleep.feature.home.experiment.JoinExperimentFragment
import com.liuxinyu.neurosleep.feature.home.label.MakeLabelFragment
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModel
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModelFactory
import com.liuxinyu.neurosleep.util.user.AuthManager
import com.liuxinyu.neurosleep.util.config.UploadConfig
import com.liuxinyu.neurosleep.util.file.EcgBinFileReader
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
import com.liuxinyu.neurosleep.data.network.FileUploadRecallItem
import com.liuxinyu.neurosleep.data.network.FileUploadRecallRequest

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

        val bufferSize = UploadConfig.CHUNK_SIZE // 每块 5MB
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

        val experimentId = 1  // 示例 ExperimentId，可为 null
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
                    // 继续上传流程
                    proceedWithUpload(uri, inputStream, fileSize, fileName, bufferSize, totalChunks, snCode, token, experimentId)
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            // 没有未完成的标签，直接上传
            proceedWithUpload(uri, inputStream, fileSize, fileName, bufferSize, totalChunks, snCode, token, experimentId)
        }
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
        experimentId: Int?
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
                // 调试：显示文件头信息
                Log.d(TAG, "=== File Upload Debug Info ===")
                EcgBinFileReader.debugFileHeader(requireContext(), uri)

                // 确保token格式正确
                val formattedToken = if (!token.startsWith("Bearer ")) "Bearer $token" else token

                // 第一步：上传状态标签（可选）
                if (UploadConfig.ENABLE_STATUS_LABEL_UPLOAD) {
                    Log.d(TAG, "Step 1: Uploading status labels")

                    // 从ECG.BIN文件头读取采集开始时间
                    val collectionStartTime = EcgBinFileReader.readCollectionStartTime(requireContext(), uri)
                    if (collectionStartTime == null) {
                        Log.e(TAG, "Failed to read collection start time from file header, using fallback time")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "无法读取文件头时间信息，使用默认时间", Toast.LENGTH_SHORT).show()
                        }
                        // 使用当前时间作为后备方案
                        val fallbackTime = LocalDateTime.now().minusHours(1)
                        Log.d(TAG, "Using fallback collection start time: $fallbackTime")
                    } else {
                        Log.d(TAG, "Successfully read collection start time from file: $collectionStartTime")
                    }
                    val finalCollectionStartTime = collectionStartTime ?: LocalDateTime.now().minusHours(1)

                    // 转换标签格式并上传
                    val statusLabelRequest = StatusLabelConverter.convertToStatusLabelRequest(
                        deviceSn = snCode,
                        collectionStartTime = finalCollectionStartTime,
                        labels = currentLabels
                    )

                    // 添加详细的请求日志
                    Log.d(TAG, "Status label request: deviceSn=$snCode, collectionStartTime=$finalCollectionStartTime")
                    Log.d(TAG, "Status labels count: ${statusLabelRequest.statusLabels.size}")
                    statusLabelRequest.statusLabels.forEachIndexed { index, label ->
                        Log.d(TAG, "Label $index: status=${label.status}, start=${label.eventStartTime}, end=${label.eventEndTime}")
                    }

                    // 调用状态标签上传接口
                    try {
                        val statusLabelResponse = retrofit.uploadStatusLabel(statusLabelRequest, formattedToken)
                        if (statusLabelResponse.code != "00000") {
                            val errorMsg = "状态标签上传失败: ${statusLabelResponse.msg}"
                            Log.e(TAG, errorMsg)
                            if (UploadConfig.CONTINUE_ON_STATUS_LABEL_FAILURE) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), "$errorMsg，继续文件上传", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                                }
                                return@launch
                            }
                        } else {
                            Log.d(TAG, "Status labels uploaded successfully")
                        }
                    } catch (e: retrofit2.HttpException) {
                        val errorMsg = "状态标签上传HTTP错误: ${e.code()}"
                        Log.e(TAG, errorMsg, e)

                        if (e.code() == 500) {
                            Log.w(TAG, "Status label API not implemented on server, skipping status label upload")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "状态标签接口暂未实现，跳过标签上传", Toast.LENGTH_SHORT).show()
                            }
                        } else if (UploadConfig.CONTINUE_ON_STATUS_LABEL_FAILURE) {
                            withContext(Dispatchers.Main) {
                                val msg = if (UploadConfig.SHOW_DETAILED_STATUS_LABEL_ERRORS) {
                                    "$errorMsg，继续文件上传"
                                } else {
                                    "状态标签上传失败，继续文件上传"
                                }
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }
                    } catch (e: Exception) {
                        val errorMsg = "状态标签上传异常: ${e.message}"
                        Log.e(TAG, errorMsg, e)

                        if (UploadConfig.CONTINUE_ON_STATUS_LABEL_FAILURE) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "$errorMsg，继续文件上传", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }
                    }
                } else {
                    Log.d(TAG, "Status label upload disabled, skipping step 1")
                }

                // 第二步：获取预签名上传URL
                Log.d(TAG, "Step 2: Getting presigned upload URLs")

                // 使用新的API请求格式
                val fileRequest = FileUploadRequest(
                    originalFilename = fileName ?: "unknown.bin",
                    contentType = "application/bin",
                    partCount = totalChunks,
                    SNCode = snCode,
                    experimentId = experimentId,
                    labelDTO = json
                )
                
                val presignedRequest = PresignedUploadRequest(
                    files = listOf(fileRequest)
                )

                val presignedResponse = retrofit.getPresignedUploadUrls(presignedRequest, formattedToken)
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
            Log.d(TAG, "Step 3: Completing multipart upload")
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
                // 调用上传回调接口
                try {
                    val recallItem = FileUploadRecallItem(
                        objectName = objectName,
                        contentType = "application/bin" // 默认类型，根据实际情况修改
                    )
                    
                    val recallRequest = FileUploadRecallRequest(
                        recallList = listOf(recallItem)
                    )
                    
                    Log.d(TAG, "Calling upload recall API with objectName: $objectName")
                    val recallResponse = retrofit.fileUploadRecall(recallRequest, token)
                    
                    if (recallResponse.code == "00000") {
                        Log.d(TAG, "Upload recall successful")
                    } else {
                        Log.w(TAG, "Upload recall failed with code: ${recallResponse.code}, message: ${recallResponse.msg}")
                    }
                } catch (e: Exception) {
                    // 记录错误但不影响主流程
                    Log.e(TAG, "Failed to call upload recall API", e)
                }
                
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

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }
}