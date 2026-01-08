package com.auth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class FileStorageUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Value("{upload.user.profile.path}")
    private String userProfilePath;


    public String storeFile(MultipartFile file, String folderPath) throws IOException {
        // Create folder if missing
        File directory = new File(folderPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Extract file info
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        String baseName = "file";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            baseName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
        } else if (originalFilename != null) {
            baseName = originalFilename;
        }

        // Append timestamp
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String newFilename = baseName + "_" + timestamp + extension;

        // Save file
        File destination = new File(directory, newFilename);
        file.transferTo(destination);

        return newFilename;
    }

}
