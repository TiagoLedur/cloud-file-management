package com.tiago.cloud_file_management;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.tiago.cloud_file_management.exceptions.FileNotFoundException;
import com.tiago.cloud_file_management.exceptions.FileUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    private static final String BUCKET_NAME = "file-management-bucket05012005";

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private S3Service s3Service;

    //UPLOAD
    @Test
    @DisplayName("Testa se o arquivo é enviado para o S3")
    void testUploadFile() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");

        String fileName = s3Service.uploadFile(mockFile);

        assertNotNull(fileName);
        verify(amazonS3).putObject(any(PutObjectRequest.class));
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
        assertEquals("Arquivo não encontrado no S3: nonexistent.txt", thrown.getMessage());
    }



    //DELETE
    @Test
    @DisplayName("Testa se o arquivo existente está sendo deletado")
    void testDeleteFile() {
        when(amazonS3.doesObjectExist(BUCKET_NAME, "test.txt")).thenReturn(true);

        s3Service.deleteFile("test.txt");

        verify(amazonS3).deleteObject(BUCKET_NAME, "test.txt");
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
