package com.tiago.cloud_file_management;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.tiago.cloud_file_management.exceptions.FileHashGenerationException;
import com.tiago.cloud_file_management.exceptions.FileNotFoundException;
import com.tiago.cloud_file_management.exceptions.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;


@Service
public class S3Service {
    private final AmazonS3 amazonS3;
    private static final String BUCKET_NAME = "file-management-bucket05012005";
    private static final String HASHES = "hashes/";

    @Autowired
    public S3Service(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new FileUploadException("Arquivo para upload não selecionado.");
        }

        String fileHash = generateFileHash(file.getInputStream());

        if (amazonS3.doesObjectExist(BUCKET_NAME, HASHES + fileHash)) {
            throw new FileUploadException("Este arquivo já foi enviado anteriormente.");
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String uniqueFileName = timestamp + "-" + file.getOriginalFilename();
        amazonS3.putObject(BUCKET_NAME, uniqueFileName, file.getInputStream(), null);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);

        amazonS3.putObject(BUCKET_NAME, HASHES + fileHash, new ByteArrayInputStream(new byte[0]), metadata);

        return "Arquivo upado para o bucket com sucesso: " + uniqueFileName;
    }

    public String generateFileHash(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new FileHashGenerationException("Erro ao calcular hash do arquivo.");
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
                 .filter(file -> !file.startsWith(HASHES))
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
            S3Object file = amazonS3.getObject(BUCKET_NAME,  fileName);
            String fileHash = generateFileHash (file.getObjectContent());

            amazonS3.deleteObject(BUCKET_NAME, HASHES + fileHash);
            amazonS3.deleteObject(BUCKET_NAME, fileName);
        } catch (AmazonS3Exception | IOException e) {
            throw new FileNotFoundException("Erro ao tentar excluir o arquivo: " + fileName);
        }
    }
}



