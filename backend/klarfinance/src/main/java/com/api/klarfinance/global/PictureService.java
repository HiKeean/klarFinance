package com.api.klarfinance.global;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PictureService {

    @Value("${app.upload.dir:images}")
    private String uploadDir;

    public String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File foto tidak boleh kosong");
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFilename = UUID.randomUUID().toString() + fileExtension;

        Path targetLocation = uploadPath.resolve(newFilename);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return newFilename;
    }

    /** Filename yang dipakai selalu hasil `saveImage` (UUID + extension) - normalize+startsWith
     * di bawah cuma jaga-jaga (defense in depth), bukan karena ada jalur input user langsung. */
    public Resource loadImage(String filename) throws IOException {
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Nama file foto tidak boleh kosong");
        }

        Path uploadPath = Paths.get(uploadDir).normalize();
        Path filePath = uploadPath.resolve(filename).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Nama file foto tidak valid");
        }

        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Nama file foto tidak valid", e);
        }
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("File foto tidak ditemukan");
        }
        return resource;
    }
}