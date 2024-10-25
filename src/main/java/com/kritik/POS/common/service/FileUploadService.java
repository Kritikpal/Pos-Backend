package com.kritik.POS.common.service;

import com.kritik.POS.restaurant.DAO.ProductFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    ProductFile uploadFile(MultipartFile multipartFile);

}
