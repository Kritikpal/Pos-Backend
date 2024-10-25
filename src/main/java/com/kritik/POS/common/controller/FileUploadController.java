package com.kritik.POS.common.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.common.route.FileRoute;
import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.restaurant.DAO.ProductFile;
import jakarta.servlet.annotation.MultipartConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(FileRoute.UPLOAD_FILE)
    public ResponseEntity<ApiResponse<ProductFile>> uploadFile(@RequestParam("imageFileName") MultipartFile file){
        ProductFile productFile = fileUploadService.uploadFile(file);
        return ResponseEntity.ok(ApiResponse.SUCCESS(productFile));
    }




}
