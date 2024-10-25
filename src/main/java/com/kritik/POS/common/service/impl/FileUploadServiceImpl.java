package com.kritik.POS.common.service.impl;

import com.kritik.POS.common.repository.FileRepository;
import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.restaurant.DAO.ProductFile;
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
    private final String UPLOAD_DIR = "uploads/";  // Folder where files will be saved
    private final ResourceLoader resourceLoader;

    @Autowired
    public FileUploadServiceImpl(FileRepository fileRepository, ResourceLoader resourceLoader) {
        this.fileRepository = fileRepository;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public ProductFile uploadFile(MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();
        String newFileName = LocalDateTime.now().toString().replace(":", "-") + "_" + originalFilename;
        Path filePath = Paths.get(UPLOAD_DIR, newFileName);
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            Files.write(filePath, multipartFile.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }

        // Save file metadata to the database
        ProductFile productFile = new ProductFile();
        productFile.setFileName(newFileName);
        productFile.setUrl(filePath.toString());  // You can change this to a URL if needed
        productFile.setUploadTime(LocalDateTime.now());
        productFile.setFileType(multipartFile.getContentType());
        return fileRepository.save(productFile);
    }

}
