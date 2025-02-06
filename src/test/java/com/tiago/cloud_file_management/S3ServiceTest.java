package com.tiago.cloud_file_management;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.tiago.cloud_file_management.exceptions.FileNotFoundException;
import com.tiago.cloud_file_management.exceptions.FileUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {
    private static final String BUCKET_NAME = "file-management-bucket05012005";
    private static final String HASHES = "hashes/";
    @Mock
    private AmazonS3 amazonS3;
    @InjectMocks
    private S3Service s3Service;

    //UPLOAD
    @Test
    @DisplayName("Testa se o arquivo é enviado para o S3 e verifica se o hash é gerado corretamente")
    void testUploadFile() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");

        InputStream inputStream = new ByteArrayInputStream("conteúdo do arquivo".getBytes());
        when(mockFile.getInputStream()).thenReturn(inputStream);

        String expectedHash = s3Service.generateFileHash(inputStream);

        when(amazonS3.doesObjectExist(eq(BUCKET_NAME), eq(HASHES + expectedHash))).thenReturn(false);

        S3Service s3ServiceMock = spy(s3Service);
        doReturn(expectedHash).when(s3ServiceMock).generateFileHash(inputStream);

        String fileName = s3ServiceMock.uploadFile(mockFile);

        assertNotNull(fileName);
        verify(amazonS3).putObject(eq(BUCKET_NAME), matches("\\d+-test.txt"), any(InputStream.class), isNull());
        verify(amazonS3).putObject(eq(BUCKET_NAME), eq(HASHES + expectedHash), any(InputStream.class), any(ObjectMetadata.class));

        verify(amazonS3).doesObjectExist(eq(BUCKET_NAME), eq(HASHES + expectedHash));
    }

    @Test
    @DisplayName("Testa a exceção ao tentar fazer upload sem um arquivo selecionado")
    void testUploadExcption() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);

        FileUploadException thrown = assertThrows(FileUploadException.class, () -> s3Service.uploadFile(mockFile));

        assertEquals("Arquivo para upload não selecionado.", thrown.getMessage());
    }

    //DOWNLOAD
    @Test
    @DisplayName("Testa se o arquivo é baixado corretamente")
    void testDownloadFile() {
        when(amazonS3.doesObjectExist(BUCKET_NAME, "test.txt")).thenReturn(true);

        S3Object mockS3Object = mock(S3Object.class);
        when(amazonS3.getObject(BUCKET_NAME, "test.txt")).thenReturn(mockS3Object);

        S3Object result = s3Service.downloadFile("test.txt");

        verify(amazonS3).getObject(BUCKET_NAME, "test.txt");

        assertNotNull(result);
        assertEquals(mockS3Object, result);
    }

    @Test
    @DisplayName("Testa a exceção ao tentar baixar arquivo inexistente")
    void testDownloadFileNotFound() {
        when(amazonS3.doesObjectExist(BUCKET_NAME, "nonexistent.txt")).thenReturn(false);

        FileNotFoundException thrown = assertThrows(FileNotFoundException.class, () -> s3Service.downloadFile("nonexistent.txt"));
        assertEquals("Arquivo não encontrado no bucket: nonexistent.txt", thrown.getMessage());
    }

    //DELETE
    @Test
    @DisplayName("Testa se o arquivo existente está sendo deletado")
    void testDeleteFile() throws IOException {
        String fileName = "existent-file.txt";
        S3Object mockFile = new S3Object();
        InputStream inputStream = new ByteArrayInputStream("conteúdo do arquivo".getBytes());
        mockFile.setObjectContent(inputStream);
        when(amazonS3.doesObjectExist(BUCKET_NAME, fileName)).thenReturn(true);
        when(amazonS3.getObject(BUCKET_NAME, fileName)).thenReturn(mockFile);

        ArgumentCaptor<String> bucketCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        s3Service.deleteFile(fileName);

        verify(amazonS3, times(2)).deleteObject(bucketCaptor.capture(), keyCaptor.capture());

        List<String> capturedKeys = keyCaptor.getAllValues();
        assertTrue(capturedKeys.contains(fileName));
        assertTrue(capturedKeys.stream().anyMatch(key -> key.startsWith(HASHES)));
    }


    @Test
    @DisplayName("Testa a exceção ao tentar deletar arquivo inexistente")
    void testDeleteFileNotFound() {
        when(amazonS3.doesObjectExist(BUCKET_NAME, "nonexistent.txt")).thenReturn(false);

        FileNotFoundException thrown = assertThrows(FileNotFoundException.class, () -> s3Service.deleteFile("nonexistent.txt"));
        assertEquals("Arquivo não encontrado para exclusão: nonexistent.txt", thrown.getMessage());
    }

    //LIST
    @Test
    @DisplayName("Testa a listagem de arquivos quando há arquivos no bucket")
    void testListFiles() {
        ListObjectsV2Result mockResult = mock(ListObjectsV2Result.class);
        S3ObjectSummary file1 = new S3ObjectSummary();
        file1.setKey("file1.txt");

        S3ObjectSummary file2 = new S3ObjectSummary();
        file2.setKey("file2.jpg");

        when(mockResult.getObjectSummaries()).thenReturn(List.of(file1, file2));
        when(amazonS3.listObjectsV2(BUCKET_NAME)).thenReturn(mockResult);

        List<String> files = s3Service.listFiles();

        assertEquals(2, files.size());
        assertTrue(files.contains("file1.txt"));
        assertTrue(files.contains("file2.jpg"));
    }

    @Test
    @DisplayName("Testa a exceção ao tentar listar arquivos quando o bucket está vazio")
    void testListFilesEmptyBucket() {
        ListObjectsV2Result emptyResult = mock(ListObjectsV2Result.class);
        when(emptyResult.getObjectSummaries()).thenReturn(List.of());
        when(amazonS3.listObjectsV2(BUCKET_NAME)).thenReturn(emptyResult);

        FileNotFoundException thrown = assertThrows(FileNotFoundException.class, () -> s3Service.listFiles());
        assertEquals("Nenhum arquivo encontrado no bucket.", thrown.getMessage());
    }

}