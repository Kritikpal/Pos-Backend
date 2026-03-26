package com.kritik.POS.common.service;

import com.kritik.POS.restaurant.entity.ProductFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    // ✅ NEW: internal usage
    ProductFile uploadFile(byte[] data, String fileName, String contentType);

    ProductFile uploadFile(MultipartFile multipartFile);

}
