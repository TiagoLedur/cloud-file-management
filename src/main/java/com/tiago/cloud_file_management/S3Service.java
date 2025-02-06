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

    /**
     * Faz o upload de um arquivo para o Amazon S3.
     *
     * @param file O arquivo a ser enviado.
     * @return Uma mensagem confirmando o sucesso do upload.
     * @throws IOException Se ocorrer um erro ao ler o arquivo.
     * @throws FileUploadException Se o arquivo já existir no bucket ou não for fornecido.
     */
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

    /**
     * Gera um hash SHA-256 para um arquivo enviado.
     *
     * @param inputStream O stream de entrada do arquivo.
     * @return O hash do arquivo codificado em Base64.
     * @throws IOException Se ocorrer um erro ao ler o arquivo.
     * @throws FileHashGenerationException Se o algoritmo SHA-256 não estiver disponível.
     */
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

    /**
     * Baixa um arquivo do Amazon S3.
     *
     * @param fileName O nome do arquivo a ser baixado.
     * @return O objeto S3 contendo o arquivo.
     * @throws FileNotFoundException Se o arquivo não for encontrado no bucket.
     * @throws AmazonS3Exception Se ocorrer um erro ao tentar baixar o arquivo.
     */
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

    /**
     * Lista todos os arquivos armazenados no bucket, excluindo arquivos de hash.
     *
     * @return Uma lista contendo os nomes dos arquivos.
     * @throws FileNotFoundException Se nenhum arquivo for encontrado.
     */
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

    /**
     * Exclui um arquivo do Amazon S3, removendo também seu hash associado.
     *
     * @param fileName O nome do arquivo a ser excluído.
     * @throws FileNotFoundException Se o arquivo não for encontrado.
     */
    public void deleteFile(String fileName) {
        if (!amazonS3.doesObjectExist(BUCKET_NAME, fileName)) {
            throw new FileNotFoundException("Arquivo não encontrado para exclusão: " + fileName);
        }
        try {
            S3Object file = amazonS3.getObject(BUCKET_NAME, fileName);
            String fileHash = generateFileHash(file.getObjectContent());

            amazonS3.deleteObject(BUCKET_NAME, HASHES + fileHash);
            amazonS3.deleteObject(BUCKET_NAME, fileName);
        } catch (AmazonS3Exception | IOException e) {
            throw new FileNotFoundException("Erro ao tentar excluir o arquivo: " + fileName);
        }
    }
}
