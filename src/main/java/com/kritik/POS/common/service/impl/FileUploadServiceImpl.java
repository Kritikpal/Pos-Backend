package com.kritik.POS.common.service.impl;

import com.kritik.POS.common.repository.FileRepository;
import com.kritik.POS.common.route.FileRoute;
import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.entity.ProductFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    private final FileRepository fileRepository;

    @Autowired
    public FileUploadServiceImpl(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public ProductFile uploadFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new AppException("Please upload a non-empty file", HttpStatus.BAD_REQUEST);
        }
        try {
            return uploadFile(
                    multipartFile.getBytes(),
                    multipartFile.getOriginalFilename(),
                    multipartFile.getContentType()
            );
        } catch (IOException exception) {
            throw new AppException("Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ProductFile uploadFile(byte[] data, String fileName, String contentType) {
        if (data == null || data.length == 0) {
            throw new AppException("File content is empty", HttpStatus.BAD_REQUEST);
        }

        String safeFileName = generateFileName(fileName);
        Path filePath = UPLOAD_DIR.resolve(safeFileName).normalize();
        if (!filePath.startsWith(UPLOAD_DIR)) {
            throw new AppException("Invalid file name", HttpStatus.BAD_REQUEST);
        }

        try {
            Files.createDirectories(UPLOAD_DIR);
            Files.write(filePath, data);
        } catch (IOException exception) {
            throw new AppException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return saveMetadata(safeFileName, contentType);
    }

    private String generateFileName(String originalFilename) {
        String cleanFileName = StringUtils.cleanPath(Objects.requireNonNullElse(originalFilename, "file.bin"));
        if (!StringUtils.hasText(cleanFileName) || cleanFileName.contains("..")) {
            throw new AppException("Invalid file name", HttpStatus.BAD_REQUEST);
        }
        return LocalDateTime.now().toString().replace(":", "-") + "_" + cleanFileName;
    }

    private ProductFile saveMetadata(String fileName, String contentType) {
        ProductFile productFile = new ProductFile();
        productFile.setFileName(fileName);
        productFile.setUrl(FileRoute.UPLOADS_BASE_PATH + "/" + fileName);
        productFile.setUploadTime(LocalDateTime.now());
        productFile.setFileType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream");
        return fileRepository.save(productFile);
    }
}
