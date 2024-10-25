package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.restaurant.DAO.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private Long categoryId;
    private String categoryName;
    private String categoryDescription;
    private Boolean isActive = true;

    public static CategoryResponse buildCategoryResponse(Category category){
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setCategoryId(category.getCategoryId());
        categoryResponse.setCategoryName(category.getCategoryName());
        categoryResponse.setCategoryDescription(category.getCategoryDescription());
        categoryResponse.setIsActive(category.getIsActive());
        return categoryResponse;
    }

}
