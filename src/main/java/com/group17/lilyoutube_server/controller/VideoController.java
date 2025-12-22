package com.group17.lilyoutube_server.controller;

import com.group17.lilyoutube_server.config.ServerConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class VideoController {

    @PostMapping("/upload-video")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            System.out.println("File Name: " + file.getOriginalFilename());
            System.out.println("File Size: " + file.getSize());

            UUID uuid = UUID.randomUUID();
            String newFileName =  uuid+"."+getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));

            file.transferTo(new File(ServerConstants.videoDir +"/" +newFileName));

            return ResponseEntity.ok(newFileName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload-picture")
    public ResponseEntity<String> uploadPicture( @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            System.out.println("File Name: " + file.getOriginalFilename());
            System.out.println("File Size: " + file.getSize());

            UUID uuid = UUID.randomUUID();
            String newFileName =  uuid+"."+getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));

            file.transferTo(new File(ServerConstants.thumbDir +"/" +newFileName));

            return ResponseEntity.ok(uuid.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    private String getFileExtension(String name) {
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf + 1);
    }

}