package com.liuxinyu.neurosleep

import com.liuxinyu.neurosleep.data.network.*
import org.junit.Test
import org.junit.Assert.*

/**
 * 测试新的预签名上传API接口
 */
class UploadApiTest {

    @Test
    fun testPresignedUploadRequest() {
        val request = PresignedUploadRequest(
            originalFilename = "test.bin",
            fileSize = 1024L,
            experimentId = 1,
            snCode = "TEST123",
            labelDTO = "{\"labels\":[]}"
        )

        assertEquals("test.bin", request.originalFilename)
        assertEquals(1024L, request.fileSize)
        assertEquals(1, request.experimentId)
        assertEquals("TEST123", request.snCode)
        assertEquals("{\"labels\":[]}", request.labelDTO)
    }

    @Test
    fun testCompleteMultipartUploadRequest() {
        val parts = listOf(
            UploadPart(1, "etag1"),
            UploadPart(2, "etag2")
        )

        val request = CompleteMultipartUploadRequest(
            objectName = "user-files/2/raw-files/test.bin",
            uploadId = "upload123",
            parts = parts
        )

        assertEquals("user-files/2/raw-files/test.bin", request.objectName)
        assertEquals("upload123", request.uploadId)
        assertEquals(2, request.parts.size)
        assertEquals(1, request.parts[0].partNumber)
        assertEquals("etag1", request.parts[0].etag)
    }

    @Test
    fun testFileUploadMeta() {
        val chunkUrls = listOf(
            ChunkPresignedUrl(1, "http://example.com/chunk1"),
            ChunkPresignedUrl(2, "http://example.com/chunk2")
        )

        val meta = FileUploadMeta(
            originalFilename = "test.bin",
            objectName = "user-files/2/raw-files/test.bin",
            presignedUrl = null,
            expiryTime = 7200,
            uploadId = "upload123",
            chunkPresignedUrls = chunkUrls
        )

        assertEquals("test.bin", meta.originalFilename)
        assertEquals("upload123", meta.uploadId)
        assertEquals(2, meta.chunkPresignedUrls.size)
        assertEquals(1, meta.chunkPresignedUrls[0].partNumber)
        assertEquals("http://example.com/chunk1", meta.chunkPresignedUrls[0].presignedUrl)
    }

    @Test
    fun testApiResponse() {
        val response = ApiResponse(
            code = "00000",
            msg = "请求正常",
            data = PresignedUploadResponse(
                filesUploadMeta = listOf(
                    FileUploadMeta(
                        originalFilename = "test.bin",
                        objectName = "user-files/2/raw-files/test.bin",
                        presignedUrl = null,
                        expiryTime = 7200,
                        uploadId = "upload123",
                        chunkPresignedUrls = listOf(
                            ChunkPresignedUrl(1, "http://example.com/chunk1")
                        )
                    )
                )
            )
        )

        assertEquals("00000", response.code)
        assertEquals("请求正常", response.msg)
        assertNotNull(response.data)
        assertEquals(1, response.data?.filesUploadMeta?.size)
    }
}
