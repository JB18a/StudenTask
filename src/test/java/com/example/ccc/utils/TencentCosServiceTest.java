package com.example.ccc.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TencentCosServiceTest {

    private TencentCosService tencentCosService;

    @Mock
    private COSClient mockCosClient;

    @Mock
    private PutObjectResult mockPutObjectResult;

    @BeforeEach
    void setUp() {
        tencentCosService = new TencentCosService(mockCosClient);
        ReflectionTestUtils.setField(tencentCosService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(tencentCosService, "domain", "https://test-bucket.cos.ap-nanjing.myqcloud.com");
    }

    @Test
    void uploadFile_shouldReturnNull_whenFileIsNull() {
        String url = tencentCosService.uploadFile(null);
        assertNull(url);
        verifyNoInteractions(mockCosClient);
    }

    @Test
    void uploadFile_shouldReturnNull_whenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.txt",
            "text/plain",
            new byte[0]
        );

        String url = tencentCosService.uploadFile(emptyFile);

        assertNull(url);
    }

    @Test
    void uploadFile_shouldReturnUrl_whenFileIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Hello World".getBytes()
        );

        when(mockCosClient.putObject(any(PutObjectRequest.class))).thenReturn(mockPutObjectResult);

        String url = tencentCosService.uploadFile(file);

        assertNotNull(url);
        assertTrue(url.contains("test-bucket.cos.ap-nanjing.myqcloud.com"));
        assertTrue(url.endsWith(".txt"));
        verify(mockCosClient).putObject(any(PutObjectRequest.class));
    }

    @Test
    void uploadFile_shouldPreserveFileExtension() throws Exception {
        MockMultipartFile jpgFile = new MockMultipartFile(
            "file",
            "photo.jpg",
            "image/jpeg",
            "fake image content".getBytes()
        );

        when(mockCosClient.putObject(any(PutObjectRequest.class))).thenReturn(mockPutObjectResult);

        String url = tencentCosService.uploadFile(jpgFile);

        assertNotNull(url);
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void uploadFile_shouldGenerateUUIDFileName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Hello World".getBytes()
        );

        when(mockCosClient.putObject(any(PutObjectRequest.class))).thenReturn(mockPutObjectResult);

        String url = tencentCosService.uploadFile(file);

        assertNotNull(url);
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        assertEquals(36, fileName.length());
        assertTrue(fileName.endsWith(".txt"));
        assertFalse(fileName.contains("-"));
    }

    @Test
    void uploadFile_shouldCallCosClientWithCorrectBucket() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Hello World".getBytes()
        );

        when(mockCosClient.putObject(any(PutObjectRequest.class))).thenReturn(mockPutObjectResult);

        tencentCosService.uploadFile(file);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockCosClient).putObject(requestCaptor.capture());
        assertEquals("test-bucket", requestCaptor.getValue().getBucketName());
    }

    @Test
    void uploadFile_shouldNotCloseExternalClient() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Hello World".getBytes()
        );

        when(mockCosClient.putObject(any(PutObjectRequest.class))).thenReturn(mockPutObjectResult);

        tencentCosService.uploadFile(file);

        verify(mockCosClient, never()).shutdown();
    }
}