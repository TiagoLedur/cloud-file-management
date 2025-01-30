package com.tiago.cloud_file_management;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.tiago.cloud_file_management.exceptions.FileNotFoundException;
import com.tiago.cloud_file_management.exceptions.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
        try {
            return amazonS3.getObject(BUCKET_NAME, fileName);
        } catch (AmazonS3Exception e) {
            throw new FileNotFoundException("Arquivo não encontrado no S3: " + fileName);
        }
    }

    public void deleteFile(String fileName) {
       try {
           if (amazonS3.doesObjectExist(BUCKET_NAME, fileName)) {
               amazonS3.deleteObject(BUCKET_NAME, fileName);
           }
       } catch (AmazonS3Exception e) {
            throw new FileNotFoundException("Arquivo não encontrado para exclusão: " + fileName);
       }
    }

}

