package com.inventory.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class FileStorageUtil {

    private final Path fileStoragePath;


//    @PostConstruct
//    public void init() {
//        try {
//            // Resolve path and create folder if not exists
//            fileStoragePath = Paths.get(fileStoragePath).toAbsolutePath().normalize();
//            Files.createDirectories(fileStoragePath);
//            log.info("📁 Upload directory created/verified: {}", fileStoragePath);
//        } catch (IOException ex) {
//            throw new RuntimeException("❌ Cannot create upload folder: " + uploadDir, ex);
//        }
//    }
    public FileStorageUtil(@Value("${inventory.file.upload-dir}") String uploadDir) {
        this.fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStoragePath);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot create upload folder: " + uploadDir, ex);
        }
    }

    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String extension = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + extension;
        Path targetLocation = this.fileStoragePath.resolve(fileName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName; // store only filename in DB
        } catch (IOException ex) {
            throw new RuntimeException("File upload failed: " + originalName, ex);
        }
    }
}
