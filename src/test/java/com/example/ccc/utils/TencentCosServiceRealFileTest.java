package com.example.ccc.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TencentCosServiceRealFileTest {

    private TencentCosService cosService;
    private String secretId;
    private String secretKey;

    @BeforeEach
    void setUp() {
        secretId = System.getenv("COS_ID");
        secretKey = System.getenv("COS_KEY");

        if (secretId == null || secretKey == null || secretId.isEmpty() || secretKey.isEmpty()) {
            System.out.println("===========================================");
            System.out.println("警告: COS_ID 或 COS_KEY 环境变量未设置!");
            System.out.println("请先设置环境变量:");
            System.out.println("  Windows: $env:COS_ID='your-secret-id'; $env:COS_KEY='your-secret-key'");
            System.out.println("  Linux/Mac: export COS_ID='your-secret-id' COS_KEY='your-secret-key'");
            System.out.println("===========================================");
        }

        cosService = new TencentCosService();
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "secretId", secretId);
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "secretKey", secretKey);
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "bucketName", "czzz-1412464270");
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "region", "ap-nanjing");
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "domain", "https://czzz-1412464270.cos.ap-nanjing.myqcloud.com");
    }

    @Test
    void uploadFile_shouldUploadRealScreenshotFile() throws Exception {
        if (secretId == null || secretKey == null || secretId.isEmpty() || secretKey.isEmpty()) {
            System.out.println("跳过测试: 环境变量未设置");
            return;
        }

        String filePath = "C:\\Users\\迷人的牛爷爷\\Pictures\\Screenshots\\屏幕截图 2025-11-25 083744.png";
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            System.out.println("测试文件不存在，请确认文件路径: " + filePath);
            assertTrue(Files.exists(path), "测试文件不存在");
            return;
        }

        byte[] fileContent = Files.readAllBytes(path);
        String originalFilename = path.getFileName().toString();

        System.out.println("文件大小: " + fileContent.length + " bytes");
        System.out.println("文件名: " + originalFilename);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            originalFilename,
            "image/png",
            fileContent
        );

        String url = cosService.uploadFile(file);

        System.out.println("上传成功! URL: " + url);

        assertNotNull(url, "URL不应为空");
        assertTrue(url.contains("czzz-1412464270.cos.ap-nanjing.myqcloud.com"),
            "URL应包含COS域名");
        assertTrue(url.endsWith(".png"), "URL应以.png结尾");
    }

    @Test
    void uploadFile_shouldUploadSmallTextFile() {
        if (secretId == null || secretKey == null || secretId.isEmpty() || secretKey.isEmpty()) {
            System.out.println("跳过测试: 环境变量未设置");
            return;
        }

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Hello Tencent COS!".getBytes()
        );

        String url = cosService.uploadFile(file);

        System.out.println("小文件上传成功! URL: " + url);

        assertNotNull(url);
        assertTrue(url.contains("czzz-1412464270.cos.ap-nanjing.myqcloud.com"));
        assertTrue(url.endsWith(".txt"));
    }
}