package com.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@Slf4j
public class FileStorageUtil {

    private final Path rootPath;

    public FileStorageUtil(
            @Value("${image.storage.root-path}") String rootPath) {

        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.rootPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create root upload folder", e);
        }
    }

    /* ================= UPLOAD ================= */

    public String upload(MultipartFile file, String type) {

        validate(file);

        String extension = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + extension;

        Path targetDir = getTypeDir(type);
        Path targetFile = targetDir.resolve(fileName);

        try {
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    /* ================= FETCH ================= */

    public byte[] fetch(String type, String fileName) {

        Path filePath = getTypeDir(type).resolve(fileName);

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found");
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file", e);
        }
    }

    /* ================= DELETE ================= */

    public void delete(String type, String fileName) {

        Path filePath = getTypeDir(type).resolve(fileName);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file", e);
        }
    }

    /* ================= UPDATE ================= */

    public String update(String type, String oldFileName, MultipartFile newFile) {

        delete(type, oldFileName);
        return upload(newFile, type);
    }

    /* ================= HELPERS ================= */

    private Path getTypeDir(String type) {
        Path path = rootPath.resolve(type.toLowerCase());
        try {
            Files.createDirectories(path);
            return path;
        } catch (IOException e) {
            throw new RuntimeException("Could not create directory: " + type, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Empty file");
        }

        if (file.getContentType() == null ||
                !file.getContentType().startsWith("image/")) {
            throw new RuntimeException("Only image files allowed");
        }
    }

    private String getExtension(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf("."));
    }
}
