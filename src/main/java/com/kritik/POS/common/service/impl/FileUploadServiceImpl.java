package com.kritik.POS.common.service.impl;

import com.kritik.POS.common.repository.FileRepository;
import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.restaurant.entity.ProductFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
@Service
public class FileUploadServiceImpl implements FileUploadService {

    private final FileRepository fileRepository;
    private final String UPLOAD_DIR = "uploads/";
    private final ResourceLoader resourceLoader;

    @Autowired
    public FileUploadServiceImpl(FileRepository fileRepository, ResourceLoader resourceLoader) {
        this.fileRepository = fileRepository;
        this.resourceLoader = resourceLoader;
    }

    // ✅ Existing (delegates to core method)
    @Override
    public ProductFile uploadFile(MultipartFile multipartFile) {
        try {
            return uploadFile(
                    multipartFile.getBytes(),
                    multipartFile.getOriginalFilename(),
                    multipartFile.getContentType()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    // ✅ NEW CORE METHOD (single source of truth)
    @Override
    public ProductFile uploadFile(byte[] data, String fileName, String contentType) {

        String safeFileName = generateFileName(fileName);
        Path filePath = Paths.get(UPLOAD_DIR, safeFileName);

        try {
            Files.createDirectories(filePath.getParent()); // always safe
            Files.write(filePath, data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        return saveMetadata(safeFileName, filePath, contentType);
    }

    // 🔹 Helper: filename generator
    private String generateFileName(String originalFilename) {
        return LocalDateTime.now().toString().replace(":", "-") + "_" + originalFilename;
    }

    // 🔹 Helper: DB save
    private ProductFile saveMetadata(String fileName, Path filePath, String contentType) {
        ProductFile productFile = new ProductFile();
        productFile.setFileName(fileName);
        productFile.setUrl(filePath.toString());
        productFile.setUploadTime(LocalDateTime.now());
        productFile.setFileType(contentType);
        return fileRepository.save(productFile);
    }
}