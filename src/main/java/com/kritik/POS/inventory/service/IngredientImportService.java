package com.kritik.POS.inventory.service;

import com.kritik.POS.inventory.models.response.IngredientImportCommitResponse;
import com.kritik.POS.inventory.models.response.IngredientImportPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IngredientImportService {

    IngredientImportPreviewResponse previewImport(MultipartFile file, Long restaurantId, Boolean overwriteNulls);

    IngredientImportCommitResponse commitImport(String previewToken);
}
