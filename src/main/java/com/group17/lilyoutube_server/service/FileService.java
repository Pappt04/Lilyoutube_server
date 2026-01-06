package com.group17.lilyoutube_server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileService {

    @Value("${app.upload.timeout-ms:60000}")
    private long uploadTimeoutMs;

    public String saveFile(MultipartFile file, String targetDir) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        UUID uuid = UUID.randomUUID();
        String extension = getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
        String newFileName = uuid + (extension.isEmpty() ? "" : "." + extension);
        File targetFile = new File(targetDir + "/" + newFileName);

        try (InputStream inputStream = file.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long startTime = System.currentTimeMillis();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (System.currentTimeMillis() - startTime > uploadTimeoutMs) {
                    throw new IOException("Upload timed out");
                }
                outputStream.write(buffer, 0, bytesRead);
            }

            return newFileName;

        } catch (IOException e) {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            throw e;
        }
    }

    public void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    private String getFileExtension(String name) {
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf + 1);
    }
}
