package com.example.ccc.utils;

import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("腾讯云COS服务综合性能测试")
class CosPerformanceBenchmarkTest {

    private TencentCosService cosService;
    private static final String TEST_BUCKET = "czzz-1412464270";
    private static final String TEST_REGION = "ap-nanjing";
    private static final String TEST_DOMAIN = "https://czzz-1412464270.cos.ap-nanjing.myqcloud.com";

    private static final List<PerformanceResult> performanceResults = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cosService = new TencentCosService();
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "secretId", System.getenv("COS_ID"));
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "secretKey", System.getenv("COS_KEY"));
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "bucketName", TEST_BUCKET);
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "region", TEST_REGION);
        org.springframework.test.util.ReflectionTestUtils.setField(cosService, "domain", TEST_DOMAIN);
    }

    @Nested
    @DisplayName("功能正确性测试")
    class FunctionalityTests {

        @Test
        @DisplayName("1. 小文本文件上传")
        void uploadSmallTextFile() {
            MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello Tencent COS!".getBytes()
            );

            String url = cosService.uploadFile(file);

            assertNotNull(url);
            assertTrue(url.contains(TEST_DOMAIN));
            assertTrue(url.endsWith(".txt"));
            System.out.println("[功能测试] 小文本文件上传成功: " + url);
        }

        @Test
        @DisplayName("2. 图片文件上传")
        void uploadImageFile() throws Exception {
            String filePath = "C:\\Users\\迷人的牛爷爷\\Pictures\\Screenshots\\屏幕截图 2025-11-25 083744.png";
            Path path = java.nio.file.Paths.get(filePath);

            if (!Files.exists(path)) {
                System.out.println("[跳过] 测试图片不存在");
                return;
            }

            byte[] content = Files.readAllBytes(path);
            MockMultipartFile file = new MockMultipartFile(
                "file", path.getFileName().toString(), "image/png", content
            );

            String url = cosService.uploadFile(file);

            assertNotNull(url);
            assertTrue(url.contains(TEST_DOMAIN));
            assertTrue(url.endsWith(".png"));
            System.out.println("[功能测试] 图片上传成功: " + url);
        }

        @Test
        @DisplayName("3. 不同扩展名文件上传")
        void uploadVariousExtensions() {
            String[][] testFiles = {
                {"document.pdf", "application/pdf", "PDF文件"},
                {"data.json", "application/json", "JSON文件"},
                {"archive.zip", "application/zip", "ZIP文件"},
                {"image.jpg", "image/jpeg", "JPG图片"}
            };

            for (String[] fileInfo : testFiles) {
                String filename = fileInfo[0];
                String mimeType = fileInfo[1];
                String desc = fileInfo[2];

                MockMultipartFile file = new MockMultipartFile(
                    "file", filename, mimeType, ("测试内容 - " + desc).getBytes()
                );

                String url = cosService.uploadFile(file);
                assertNotNull(url, desc + " 上传失败");
                System.out.println("[功能测试] " + desc + " 上传成功");
            }
        }

        @Test
        @DisplayName("4. 空文件和null处理")
        void handleNullAndEmptyFiles() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
            );

            assertNull(cosService.uploadFile(null));
            assertNull(cosService.uploadFile(emptyFile));
            System.out.println("[功能测试] 空文件和null处理正确");
        }

        @Test
        @DisplayName("5. UUID文件名生成唯一性")
        void generateUniqueFileNames() {
            Set<String> fileNames = new HashSet<>();
            MockMultipartFile file = new MockMultipartFile(
                "file", "same.txt", "text/plain", "same content".getBytes()
            );

            for (int i = 0; i < 100; i++) {
                String url = cosService.uploadFile(file);
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                fileNames.add(fileName);
            }

            assertEquals(100, fileNames.size(), "每次上传应生成唯一的文件名");
            System.out.println("[功能测试] 100次上传生成了100个唯一文件名");
        }
    }

    @Nested
    @DisplayName("性能基准测试")
    class PerformanceBenchmarkTests {

        @Test
        @DisplayName("6. 不同大小文件的传输性能")
        void benchmarkDifferentFileSizes() throws Exception {
            long[] fileSizes = {1024, 10 * 1024, 100 * 1024, 1024 * 1024}; // 1KB, 10KB, 100KB, 1MB

            System.out.println("\n========== 文件大小性能测试 ==========");
            System.out.printf("%-15s %-15s %-15s %-15s%n", "文件大小", "上传时间(ms)", "传输速度(KB/s)", "状态");
            System.out.println("----------------------------------------");

            for (long size : fileSizes) {
                byte[] data = new byte[(int) size];
                Arrays.fill(data, (byte) 'A');

                MockMultipartFile file = new MockMultipartFile(
                    "file", "benchmark_" + size + ".bin", "application/octet-stream", data
                );

                long startTime = System.nanoTime();
                String url = cosService.uploadFile(file);
                long endTime = System.nanoTime();

                double uploadTimeMs = (endTime - startTime) / 1_000_000.0;
                double speedKBps = size / 1024.0 / (uploadTimeMs / 1000.0);

                System.out.printf("%-15s %-15.2f %-15.2f %-15s%n",
                    formatSize(size), uploadTimeMs, speedKBps, "成功");

                performanceResults.add(new PerformanceResult("上传", size, uploadTimeMs, speedKBps));
            }
        }

        @Test
        @DisplayName("7. 连续100次上传稳定性测试")
        void stabilityTest() {
            int iterations = 100;
            List<Long> uploadTimes = new ArrayList<>();
            MockMultipartFile file = new MockMultipartFile(
                "file", "stability_test.txt", "text/plain", "stability test content".getBytes()
            );

            System.out.println("\n========== 稳定性测试 (100次连续上传) ==========");

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                String url = cosService.uploadFile(file);
                long endTime = System.nanoTime();

                assertNotNull(url);
                uploadTimes.add((endTime - startTime) / 1_000_000);

                if ((i + 1) % 20 == 0) {
                    System.out.println("已完成: " + (i + 1) + "/" + iterations);
                }
            }

            double avg = uploadTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            double min = uploadTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            double max = uploadTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            double stdDev = calculateStdDev(uploadTimes, avg);

            System.out.println("\n========== 稳定性统计 ==========");
            System.out.printf("平均耗时: %.2f ms%n", avg);
            System.out.printf("最快耗时: %.2f ms%n", min);
            System.out.printf("最慢耗时: %.2f ms%n", max);
            System.out.printf("标准差:   %.2f ms%n", stdDev);
            System.out.printf("变异系数: %.2f%%%n", (stdDev / avg) * 100);
        }

        @Test
        @DisplayName("8. 模拟本地存储vs云存储对比")
        void compareLocalVsCloudStorage() throws Exception {
            System.out.println("\n========== 本地存储 vs 云存储对比测试 ==========");

            int iterations = 10;
            byte[] testData = new byte[100 * 1024]; // 100KB

            // 云存储上传测试
            MockMultipartFile cloudFile = new MockMultipartFile(
                "file", "cloud_test.bin", "application/octet-stream", testData
            );

            long cloudTotalTime = 0;
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                cosService.uploadFile(cloudFile);
                long end = System.nanoTime();
                cloudTotalTime += (end - start);
            }
            double cloudAvgMs = (cloudTotalTime / iterations) / 1_000_000.0;

            // 本地存储模拟 (写入临时文件再删除)
            Path tempDir = Files.createTempDirectory("local_storage_test");
            long localTotalTime = 0;

            try {
                for (int i = 0; i < iterations; i++) {
                    Path tempFile = tempDir.resolve("local_" + i + ".bin");
                    long start = System.nanoTime();
                    Files.write(tempFile, testData);
                    long end = System.nanoTime();
                    localTotalTime += (end - start);
                    Files.deleteIfExists(tempFile);
                }
            } finally {
                Files.walk(tempDir).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }

            double localAvgMs = (localTotalTime / iterations) / 1_000_000.0;

            System.out.println("\n[100KB文件 " + iterations + "次测试平均值]");
            System.out.println("----------------------------------------");
            System.out.printf("本地存储平均耗时: %.2f ms%n", localAvgMs);
            System.out.printf("云存储平均耗时:   %.2f ms%n", cloudAvgMs);
            System.out.println("----------------------------------------");

            double ratio = cloudAvgMs / localAvgMs;
            String comparison;
            if (ratio < 1) {
                comparison = String.format("云存储比本地快 %.2fx", 1 / ratio);
            } else {
                comparison = String.format("本地存储比云存储快 %.2fx", ratio);
            }
            System.out.println("对比结论: " + comparison);

            performanceResults.add(new PerformanceResult("本地存储100KB", 100 * 1024, localAvgMs, 0));
            performanceResults.add(new PerformanceResult("云存储100KB", 100 * 1024, cloudAvgMs, 0));

            System.out.println("\n[说明] 实际生产环境中，云存储优势在于:");
            System.out.println("  1. 无需占用服务器本地磁盘空间");
            System.out.println("  2. 支持CDN加速分发");
            System.out.println("  3. 数据持久性更高(99.999999999%)");
            System.out.println("  4. 可扩展性强");
            System.out.println("  5. 便于多节点共享访问");
        }

        @Test
        @DisplayName("9. 网络延迟对云存储的影响测试")
        void testNetworkLatencyImpact() {
            System.out.println("\n========== 网络延迟影响测试 ==========");

            // 执行多次上传测量网络波动
            int testCount = 20;
            List<Long> times = new ArrayList<>();
            MockMultipartFile file = new MockMultipartFile(
                "file", "latency_test.txt", "text/plain", "network latency test".getBytes()
            );

            for (int i = 0; i < testCount; i++) {
                long start = System.nanoTime();
                cosService.uploadFile(file);
                long end = System.nanoTime();
                times.add((end - start) / 1_000_000);
            }

            double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
            double p50 = getPercentile(times, 50);
            double p90 = getPercentile(times, 90);
            double p99 = getPercentile(times, 99);

            System.out.printf("平均延迟: %.2f ms%n", avg);
            System.out.printf("P50延迟:  %.2f ms%n", p50);
            System.out.printf("P90延迟:  %.2f ms%n", p90);
            System.out.printf("P99延迟:  %.2f ms%n", p99);
            System.out.println("----------------------------------------");
            System.out.println("P99延迟表示99%的请求都在此时间内完成");
        }

        @Test
        @DisplayName("10. 完整端到端集成测试")
        void endToEndIntegrationTest() throws Exception {
            System.out.println("\n========== 端到端集成测试 ==========");

            String filePath = "C:\\Users\\迷人的牛爷爷\\Pictures\\Screenshots\\屏幕截图 2025-11-25 083744.png";
            Path path = java.nio.file.Paths.get(filePath);

            if (!Files.exists(path)) {
                System.out.println("[跳过] 测试图片不存在");
                return;
            }

            byte[] content = Files.readAllBytes(path);
            String originalFilename = path.getFileName().toString();

            System.out.println("原始文件: " + originalFilename);
            System.out.println("文件大小: " + formatSize(content.length));

            long startTime = System.currentTimeMillis();
            MockMultipartFile file = new MockMultipartFile(
                "file", originalFilename, "image/png", content
            );
            String url = cosService.uploadFile(file);
            long endTime = System.currentTimeMillis();

            double totalTime = endTime - startTime;
            double speedMBps = (content.length / (1024.0 * 1024.0)) / (totalTime / 1000.0);

            System.out.println("上传耗时: " + String.format("%.2f", totalTime) + " ms");
            System.out.println("传输速度: " + String.format("%.2f", speedMBps) + " MB/s");
            System.out.println("访问URL: " + url);
            System.out.println("========================================");

            assertNotNull(url);
            assertTrue(url.contains(TEST_DOMAIN));
        }
    }

    @AfterAll
    static void printSummary() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              腾讯云COS性能测试报告总结                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        if (!performanceResults.isEmpty()) {
            System.out.println("\n[测试项目概览]");
            System.out.printf("%-25s %-15s %-15s %-15s%n", "操作类型", "文件大小", "耗时(ms)", "速度(KB/s)");
            System.out.println("----------------------------------------------------------------");
            for (PerformanceResult r : performanceResults) {
                System.out.printf("%-25s %-15s %-15.2f %-15.2f%n",
                    r.operation, formatSize(r.fileSize), r.timeMs, r.speedKBps);
            }
        }

        System.out.println("\n[测试方法说明]");
        System.out.println("性能测试采用以下方法确保准确性:");
        System.out.println("  1. 使用System.nanoTime()获取高精度时间戳");
        System.out.println("  2. 多次运行取平均值消除偶发波动");
        System.out.println("  3. 计算标准差和百分位数评估稳定性");
        System.out.println("  4. 对比本地存储baseline评估相对性能");

        System.out.println("\n[云存储vs本地存储]");
        System.out.println("理论上云存储单次请求会比本地磁盘慢(受网络延迟影响),");
        System.out.println("但云存储的优势在于:");
        System.out.println("  ✓ 不占用服务器本地空间");
        System.out.println("  ✓ 支持全球CDN加速访问");
        System.out.println("  ✓ 数据冗余备份,可靠性99.999999999%");
        System.out.println("  ✓ 无需扩容,按需使用");
        System.out.println("  ✓ 多节点共享,适合分布式系统");
        System.out.println("========================================\n");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private double calculateStdDev(List<Long> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0);
        return Math.sqrt(variance);
    }

    private double getPercentile(List<Long> sortedValues, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        Collections.sort(sortedValues);
        return sortedValues.get(Math.max(0, index));
    }

    static class PerformanceResult {
        String operation;
        long fileSize;
        double timeMs;
        double speedKBps;

        PerformanceResult(String operation, long fileSize, double timeMs, double speedKBps) {
            this.operation = operation;
            this.fileSize = fileSize;
            this.timeMs = timeMs;
            this.speedKBps = speedKBps;
        }
    }
}