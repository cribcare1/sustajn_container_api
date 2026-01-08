package com.auth.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FolderCreatorBean {

    @Value("{upload.user.profile.path}")
    private String userProfilePath;

    @PostConstruct
    public void createFolders() {
        createFolder(userProfilePath);
    }
    private void createFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
            System.err.println(folder.getName());
        }
    }
}
