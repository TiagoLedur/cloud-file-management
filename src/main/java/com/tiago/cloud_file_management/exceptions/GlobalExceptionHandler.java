package com.tiago.cloud_file_management.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Trata erro de arquivo não encontrado
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<String> handleFileNotFoundException(FileNotFoundException ex, HttpServletRequest request) {
        return new ResponseEntity<>("Arquivo não encontrado: " + ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // Exceção de upload de arquivo
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<String> handleFileUploadException(FileUploadException ex, HttpServletRequest request) {
        return new ResponseEntity<>("Erro no upload do arquivo: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // Trata erro de falha na comunicação com o S3
    @ExceptionHandler(AmazonS3Exception.class)
    public ResponseEntity<String> handleAmazonS3Error(AmazonS3Exception ex) {
        String message = "Erro ao acessar o serviço de armazenamento: " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
    }

    // Trata erros genéricos de I/O
    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException ex) {
        String message = "Erro de comunicação com o servidor de arquivos: " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        String message = "O arquivo enviado excede o tamanho máximo permitido.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }


    // Para outras exceções não tratadas, retorna 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        String message = "Erro inesperado: " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
    }


}

