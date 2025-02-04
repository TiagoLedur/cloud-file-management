package com.tiago.cloud_file_management;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.tiago.cloud_file_management.exceptions.FileNotFoundException;
import com.tiago.cloud_file_management.exceptions.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@Service
public class S3Service {
    private final AmazonS3 amazonS3;
    private static final String BUCKET_NAME = "file-management-bucket05012005";

    @Autowired
    public S3Service(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if(file.isEmpty()){
            throw new FileUploadException("Arquivo para upload não selecionado.");
        }
        else {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            amazonS3.putObject(new PutObjectRequest(BUCKET_NAME, fileName, file.getInputStream(), new ObjectMetadata()));
            return fileName;
        }
    }

    public S3Object downloadFile(String fileName) {
        if (!amazonS3.doesObjectExist(BUCKET_NAME, fileName)) {
            throw new FileNotFoundException("Arquivo não encontrado no bucket: " + fileName);
        }
        try {
            return amazonS3.getObject(BUCKET_NAME, fileName);
        } catch (AmazonS3Exception e) {
            throw new AmazonS3Exception("Erro ao tentar baixar o arquivo: " + fileName);
        }
    }


    public List<String> listFiles() {
        ListObjectsV2Result result = amazonS3.listObjectsV2(BUCKET_NAME);
         List<String> files = result.getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey)
                .toList();
         if (files.isEmpty()) {
             throw new FileNotFoundException("Nenhum arquivo encontrado no bucket.");
         }
         return files;
    }


    public void deleteFile(String fileName) {
        if (!amazonS3.doesObjectExist(BUCKET_NAME, fileName)) {
            throw new FileNotFoundException("Arquivo não encontrado para exclusão: " + fileName);
        }
        try {
            amazonS3.deleteObject(BUCKET_NAME, fileName);
        } catch (AmazonS3Exception e) {
            throw new FileNotFoundException("Erro ao tentar excluir o arquivo: " + fileName);
        }
    }


}



