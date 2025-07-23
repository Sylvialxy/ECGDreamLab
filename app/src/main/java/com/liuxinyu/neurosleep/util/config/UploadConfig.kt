package com.liuxinyu.neurosleep.util.config

/**
 * 上传功能配置类
 */
object UploadConfig {
    
    /**
     * 是否启用状态标签上传功能
     * 如果后端接口未实现，可以设置为false来跳过状态标签上传
     */
    const val ENABLE_STATUS_LABEL_UPLOAD = true
    
    /**
     * 状态标签上传失败时是否继续文件上传
     * true: 状态标签上传失败时继续文件上传
     * false: 状态标签上传失败时中断整个上传流程
     */
    const val CONTINUE_ON_STATUS_LABEL_FAILURE = true
    
    /**
     * 是否在状态标签上传失败时显示详细错误信息
     */
    const val SHOW_DETAILED_STATUS_LABEL_ERRORS = true
    
    /**
     * 默认采样率（Hz）
     */
    const val DEFAULT_SAMPLING_RATE = 200
    
    /**
     * 文件上传分块大小（字节）
     */
    const val CHUNK_SIZE = 5 * 1024 * 1024 // 5MB
}
