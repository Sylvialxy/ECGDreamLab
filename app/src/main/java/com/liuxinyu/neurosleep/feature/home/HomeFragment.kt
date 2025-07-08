package com.liuxinyu.neurosleep.feature.home

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.liuxinyu.neurosleep.data.repository.EcgLabelRepository
import com.liuxinyu.neurosleep.databinding.FragmentHomeBinding
import com.liuxinyu.neurosleep.feature.home.experiment.JoinExperimentFragment
import com.liuxinyu.neurosleep.feature.home.label.MakeLabelFragment
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModel
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModelFactory
import com.liuxinyu.neurosleep.util.user.AuthManager
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

    private var selectedFile: File? = null

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

        val bufferSize = 1 * 1024 * 1024 // 每块 1MB
        val totalChunks = ((fileSize + bufferSize - 1) / bufferSize).toInt()

        val snCode = "SN123456"  // 示例 SNCode

        val token = AuthManager.getToken(requireContext())
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token is null or empty")
            Toast.makeText(requireContext(), "Token is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val experimentId = 1     // 示例 ExperimentId，可为 null
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

        val retrofit = RetrofitClient.instance.create(UserApiService::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
            inputStream.use { stream ->
                val buffer = ByteArray(bufferSize)
                var bytesRead: Int
                var chunkIndex = 0
                    var totalBytesUploaded = 0L

                while (true) {
                    bytesRead = stream.read(buffer)
                    if (bytesRead <= 0) break

                    val chunkBytes = buffer.copyOf(bytesRead)
                        totalBytesUploaded += bytesRead
                        
                        // 计算上传进度
                        val progress = (totalBytesUploaded * 100 / fileSize).toInt()
                        Log.d(TAG, "Uploading chunk $chunkIndex of $totalChunks (${progress}%)")
                        
                    val filePart = MultipartBody.Part.createFormData(
                        "file", "$fileName.part$chunkIndex",
                        chunkBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                    )

                    val chunkIndexBody = chunkIndex.toString().toRequestBody("text/plain".toMediaType())
                    val totalChunksBody = totalChunks.toString().toRequestBody("text/plain".toMediaType())
                    val snCodeBody = snCode.toRequestBody("text/plain".toMediaType())
                    val experimentIdBody = experimentId?.toString()
                        ?.toRequestBody("text/plain".toMediaType())

                    val labelPart = if (chunkIndex == totalChunks - 1) {
                            Log.d(TAG, "Uploading final chunk with label data")
                        val json = Gson().toJson(labelDTO)
                        MultipartBody.Part.createFormData(
                            "labelDTO", "labelDTO.json",
                            json.toRequestBody("application/json".toMediaType())
                        )
                    } else null

                    try {
                        val response = retrofit.uploadChunk(
                            file = filePart,
                            experimentId = experimentIdBody,
                            chunkIndex = chunkIndexBody,
                            totalChunks = totalChunksBody,
                            snCode = snCodeBody,
                            labelDTO = labelPart,
                            token
                        )
                        withContext(Dispatchers.Main) {
                                Log.d(TAG, "Chunk $chunkIndex uploaded successfully")
                                // 更新进度
                                val progressText = "上传进度: $progress%"
                                Toast.makeText(requireContext(), progressText, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "Chunk upload failed", e)
                                Toast.makeText(requireContext(), "分块 $chunkIndex 上传失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        break
                    }

                    chunkIndex++
                }

                    // 所有分块上传成功后，清空标签数据
                    viewModel.clearSession()
                    
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "File upload completed successfully")
                        Toast.makeText(requireContext(), "上传完成", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Upload failed", e)
                    Toast.makeText(requireContext(), "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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