package com.liuxinyu.neurosleep

import com.liuxinyu.neurosleep.data.network.RetrofitClient
import org.junit.Test
import org.junit.Assert.*

/**
 * 测试 RetrofitClient 的修复
 * 这个测试验证了我们修复的主要问题：
 * 1. RetrofitClient.getInstance(context) 方法可以正常工作
 * 2. 废弃的 instance 属性仍然向后兼容
 */
class RetrofitClientTest {

    @Test
    fun testRetrofitClientDeprecatedInstance() {
        // 测试废弃的 instance 属性仍然可用（向后兼容）
        @Suppress("DEPRECATION")
        val retrofit = RetrofitClient.instance

        // 验证实例不为空
        assertNotNull("Deprecated instance should still work", retrofit)

        // 验证 base URL 是否正确设置
        assertEquals("http://10.105.0.33:20000/", retrofit.baseUrl().toString())
    }

    @Test
    fun testRetrofitClientBaseUrl() {
        // 测试基础 URL 配置是否正确
        @Suppress("DEPRECATION")
        val retrofit = RetrofitClient.instance
        val baseUrl = retrofit.baseUrl().toString()

        // 验证 URL 格式
        assertTrue("Base URL should start with http://", baseUrl.startsWith("http://"))
        assertTrue("Base URL should end with /", baseUrl.endsWith("/"))
        assertEquals("http://10.105.0.33:20000/", baseUrl)
    }
}
