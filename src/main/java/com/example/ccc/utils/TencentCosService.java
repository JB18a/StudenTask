package com.example.ccc.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class TencentCosService {

    @Value("${tencent.cos.secretId}")
    private String secretId;

    @Value("${tencent.cos.secretKey}")
    private String secretKey;

    @Value("${tencent.cos.bucketName}")
    private String bucketName;

    @Value("${tencent.cos.region}")
    private String region;

    @Value("${tencent.cos.domain}")
    private String domain;

    private COSClient cosClient;

    public TencentCosService() {
    }

    public TencentCosService(COSClient cosClient) {
        this.cosClient = cosClient;
    }

    protected COSClient createCosClient() {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        Region cosRegion = new Region(region);
        ClientConfig clientConfig = new ClientConfig(cosRegion);
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(credentials, clientConfig);
    }

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;

        COSClient client = cosClient != null ? cosClient : createCosClient();
        boolean clientCreatedByUs = cosClient == null;

        try (InputStream inputStream = file.getInputStream()) {
            com.qcloud.cos.model.ObjectMetadata metadata = new com.qcloud.cos.model.ObjectMetadata();
            metadata.setContentLength(file.getSize());
            com.qcloud.cos.model.PutObjectRequest putObjectRequest =
                new com.qcloud.cos.model.PutObjectRequest(bucketName, fileName, inputStream, metadata);
            client.putObject(putObjectRequest);

            return domain + "/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } finally {
            if (clientCreatedByUs) {
                client.shutdown();
            }
        }
    }

    public byte[] downloadFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new RuntimeException("文件URL不能为空");
        }

        String fileName = extractFileNameFromUrl(fileUrl);

        COSClient client = cosClient != null ? cosClient : createCosClient();
        boolean clientCreatedByUs = cosClient == null;

        try {
            com.qcloud.cos.model.GetObjectRequest getObjectRequest =
                new com.qcloud.cos.model.GetObjectRequest(bucketName, fileName);
            com.qcloud.cos.model.COSObject cosObject = client.getObject(getObjectRequest);
            try (InputStream inputStream = cosObject.getObjectContent()) {
                return inputStream.readAllBytes();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        } finally {
            if (clientCreatedByUs) {
                client.shutdown();
            }
        }
    }

    public String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        int lastSlashIndex = fileUrl.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlashIndex + 1);
        }
        return fileUrl;
    }

    public String getFileNameFromUrl(String fileUrl) {
        return extractFileNameFromUrl(fileUrl);
    }

    public boolean isPdfFile(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    public boolean isTextFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") ||
               lower.endsWith(".doc") || lower.endsWith(".docx");
    }
}