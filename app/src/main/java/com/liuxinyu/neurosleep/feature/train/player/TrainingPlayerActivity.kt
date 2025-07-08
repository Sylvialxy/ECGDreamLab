package com.liuxinyu.neurosleep.feature.train.player

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.liuxinyu.neurosleep.R

class TrainingPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRAINING_ID = "extra_training_id"
        const val EXTRA_TRAINING_TITLE = "extra_training_title"
        const val EXTRA_TRAINING_DESCRIPTION = "extra_training_description"
        const val EXTRA_TRAINING_AUDIO_RES_ID = "extra_training_audio_res_id"
        private const val FORWARD_TIME = 15000 // 15 seconds in milliseconds
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var textTitle: TextView
    private lateinit var textDescription: TextView
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var buttonPlayPause: ImageButton
    private lateinit var buttonStop: ImageButton
    private lateinit var buttonForward: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var isMediaPlayerInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_player)
        
        // 启用返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize views
        textTitle = findViewById(R.id.textTitle)
        textDescription = findViewById(R.id.textDescription)
        textCurrentTime = findViewById(R.id.textCurrentTime)
        textTotalTime = findViewById(R.id.textTotalTime)
        seekBar = findViewById(R.id.seekBar)
        buttonPlayPause = findViewById(R.id.buttonPlayPause)
        buttonStop = findViewById(R.id.buttonStop)
        buttonForward = findViewById(R.id.buttonForward)

        // Get extras from intent
        val trainingId = intent.getIntExtra(EXTRA_TRAINING_ID, -1)
        val title = intent.getStringExtra(EXTRA_TRAINING_TITLE) ?: "Unknown Training"
        val description = intent.getStringExtra(EXTRA_TRAINING_DESCRIPTION) ?: ""
        val audioResId = intent.getIntExtra(EXTRA_TRAINING_AUDIO_RES_ID, 0)

        // 设置标题
        supportActionBar?.title = title
        
        // Set training info
        textTitle.text = title
        textDescription.text = description

        // Always initialize mediaPlayer to avoid lateinit property not initialized
        mediaPlayer = MediaPlayer()

        // Setup media player
        if (setupMediaPlayer(audioResId)) {
            // Setup controls only if media player setup was successful
            setupControls()
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
    
    override fun onBackPressed() {
        // 确保正确释放资源并返回
        if (isMediaPlayerInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
            isMediaPlayerInitialized = false
        }
        finish()
    }

    private fun setupMediaPlayer(audioResId: Int): Boolean {
        try {
            // Try to use the provided audio resource ID, fall back to sample_audio if available
            val mp = if (audioResId != 0) {
                MediaPlayer.create(this, audioResId)
            } else {
                MediaPlayer.create(this, R.raw.sample_audio)
            }
            
            // If no audio is available, show a message and return false
            if (mp == null) {
                Toast.makeText(this, "音频资源暂不可用", Toast.LENGTH_SHORT).show()
                finish()
                return false
            }
            
            // Release any existing media player
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.release()
            }
            
            // Assign the new media player
            mediaPlayer = mp
            isMediaPlayerInitialized = true
            
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                buttonPlayPause.setImageResource(android.R.drawable.ic_media_play)
                mediaPlayer.seekTo(0)
            }
            
            // Set total duration
            val totalDuration = mediaPlayer.duration
            textTotalTime.text = formatTime(totalDuration)
            
            // Setup seekbar
            seekBar.max = totalDuration
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && isMediaPlayerInitialized) {
                        mediaPlayer.seekTo(progress)
                        textCurrentTime.text = formatTime(progress)
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            
            // Start update progress
            updateSeekBar()
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "音频播放初始化失败", Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
    }

    private fun setupControls() {
        buttonPlayPause.setOnClickListener {
            if (!isMediaPlayerInitialized) return@setOnClickListener
            
            if (isPlaying) {
                mediaPlayer.pause()
                buttonPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayer.start()
                buttonPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
            isPlaying = !isPlaying
        }

        buttonStop.setOnClickListener {
            if (!isMediaPlayerInitialized) return@setOnClickListener
            
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
            seekBar.progress = 0
            textCurrentTime.text = formatTime(0)
            isPlaying = false
            buttonPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }

        buttonForward.setOnClickListener {
            if (!isMediaPlayerInitialized) return@setOnClickListener
            
            // Skip forward 15 seconds
            val currentPosition = mediaPlayer.currentPosition
            val totalDuration = mediaPlayer.duration
            val newPosition = minOf(currentPosition + FORWARD_TIME, totalDuration)
            
            mediaPlayer.seekTo(newPosition)
            seekBar.progress = newPosition
            textCurrentTime.text = formatTime(newPosition)
            
            // Show a toast message
            Toast.makeText(this, "快进15秒", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    if (isMediaPlayerInitialized && mediaPlayer.isPlaying) {
                        val currentPosition = mediaPlayer.currentPosition
                        seekBar.progress = currentPosition
                        textCurrentTime.text = formatTime(currentPosition)
                    }
                    handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 1000)
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (isMediaPlayerInitialized) {
            mediaPlayer.release()
        }
    }
} 