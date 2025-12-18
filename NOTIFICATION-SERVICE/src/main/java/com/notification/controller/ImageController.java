package com.notification.controller;

import com.notification.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageUtil storage;

    @PostMapping("/{type}")
    public String upload(@PathVariable String type,
                         @RequestParam MultipartFile file) {
        return storage.upload(file, type);
    }

    @GetMapping("/{type}/{fileName}")
    public ResponseEntity<byte[]> fetch(
            @PathVariable String type,
            @PathVariable String fileName) {

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(storage.fetch(type, fileName));
    }

    @PutMapping("/{type}/{fileName}")
    public String update(@PathVariable String type,
                         @PathVariable String fileName,
                         @RequestParam MultipartFile file) {
        return storage.update(type, fileName, file);
    }

    @DeleteMapping("/{type}/{fileName}")
    public void delete(@PathVariable String type,
                       @PathVariable String fileName) {
        storage.delete(type, fileName);
    }
}

