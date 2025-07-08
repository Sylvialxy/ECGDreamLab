package com.liuxinyu.neurosleep.feature.train.player

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.liuxinyu.neurosleep.R

class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRAINING_ID = "extra_training_id"
        const val EXTRA_TRAINING_TITLE = "extra_training_title"
        const val EXTRA_TRAINING_DESCRIPTION = "extra_training_description"
        const val EXTRA_TRAINING_VIDEO_RES_ID = "extra_training_video_res_id"
    }

    private lateinit var videoView: VideoView
    private lateinit var textTitle: TextView
    private lateinit var textDescription: TextView
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        // 启用返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 初始化视图
        videoView = findViewById(R.id.videoView)
        textTitle = findViewById(R.id.textTitle)
        textDescription = findViewById(R.id.textDescription)
        loadingText = findViewById(R.id.loadingText)

        // 获取Intent传递的数据
        val trainingId = intent.getIntExtra(EXTRA_TRAINING_ID, -1)
        val title = intent.getStringExtra(EXTRA_TRAINING_TITLE) ?: "未知训练"
        val description = intent.getStringExtra(EXTRA_TRAINING_DESCRIPTION) ?: ""
        val videoResId = intent.getIntExtra(EXTRA_TRAINING_VIDEO_RES_ID, 0)

        // 设置标题
        supportActionBar?.title = title
        
        // 设置训练信息
        textTitle.text = title
        textDescription.text = description

        // 设置视频控制器
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        // 设置视频完成监听器
        videoView.setOnCompletionListener {
            loadingText.visibility = View.GONE
        }

        // 设置视频准备就绪监听器
        videoView.setOnPreparedListener {
            loadingText.visibility = View.GONE
            // 开始播放视频
            videoView.start()
        }

        // 显示加载中文本
        loadingText.visibility = View.VISIBLE

        try {
            // 尝试加载视频资源
            if (videoResId != 0) {
                val videoUri = Uri.parse("android.resource://${packageName}/${videoResId}")
                videoView.setVideoURI(videoUri)
            } else {
                // 如果没有视频资源，显示错误消息
                loadingText.text = "视频资源不可用"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadingText.text = "视频加载失败"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 处理返回按钮点击
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        // 暂停视频播放
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止视频播放并释放资源
        videoView.stopPlayback()
    }
} 