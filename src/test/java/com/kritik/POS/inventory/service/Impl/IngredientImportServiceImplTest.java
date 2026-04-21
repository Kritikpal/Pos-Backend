package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.models.response.IngredientImportCommitResponse;
import com.kritik.POS.inventory.models.response.IngredientImportPreviewResponse;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.SupplierRepository;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientImportServiceImplTest {

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TenantAccessService tenantAccessService;

    private IngredientImportServiceImpl ingredientImportService;

    @BeforeEach
    void setUp() {
        ingredientImportService = new IngredientImportServiceImpl(
                ingredientStockRepository,
                supplierRepository,
                inventoryService,
                tenantAccessService,
                new IngredientImportPreviewStore()
        );
        when(tenantAccessService.currentUser()).thenReturn(new SecurityUser(
                99L,
                "admin@example.com",
                "token",
                "token-id",
                1L,
                1L,
                Set.of("RESTAURANT_ADMIN")
        ));
        when(tenantAccessService.resolveManageableRestaurantId(1L)).thenReturn(1L);
        when(supplierRepository.findAllByRestaurantIdAndIsDeletedFalse(1L)).thenReturn(List.of());
    }

    @Test
    void previewImportClassifiesCreateAndUpdateRows() {
        IngredientStock existing = ingredient("ING0001", "Turmeric Powder", "Spice");
        when(ingredientStockRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(existing));

        IngredientImportPreviewResponse response = ingredientImportService.previewImport(csvFile("""
                sku,ingredient_name,description,restaurant_id,total_stock,unit_code,category
                ING0001,Turmeric Powder,Updated,1,,KG,Spice
                ING0200,Fresh Curd,Daily dairy,1,10.0,KG,Dairy
                """), 1L, false);

        assertThat(response.totalRowsRead()).isEqualTo(2);
        assertThat(response.validRowCount()).isEqualTo(2);
        assertThat(response.invalidRowCount()).isZero();
        assertThat(response.rowsToUpdate()).isEqualTo(1);
        assertThat(response.rowsToCreate()).isEqualTo(1);
        assertThat(response.rows()).extracting("action").containsExactly("UPDATE", "CREATE");
        assertThat(response.previewToken()).isNotBlank();
    }

    @Test
    void previewImportRejectsDuplicateSkuAndRestaurantMismatch() {
        when(ingredientStockRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of());

        IngredientImportPreviewResponse response = ingredientImportService.previewImport(csvFile("""
                sku,ingredient_name,description,restaurant_id,total_stock,unit_code,category
                ING0001,Turmeric Powder,Updated,1,2.0,KG,Spice
                ING0001,Red Chilli Powder,Updated,1,2.0,KG,Spice
                ING0002,Fresh Curd,Daily dairy,2,10.0,KG,Dairy
                """), 1L, false);

        assertThat(response.totalRowsRead()).isEqualTo(3);
        assertThat(response.validRowCount()).isZero();
        assertThat(response.invalidRowCount()).isEqualTo(3);
        assertThat(response.rows()).allMatch(row -> "ERROR".equals(row.action()));
        assertThat(response.rows().get(0).errors()).contains("Duplicate sku found in the import file");
        assertThat(response.rows().get(2).errors()).contains("restaurant_id must match the selected restaurant");
    }

    @Test
    void commitImportRejectsInvalidToken() {
        assertThatThrownBy(() -> ingredientImportService.commitImport("missing-token"))
                .isInstanceOf(AppException.class)
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void commitImportUpsertsRowsAndPreservesBlankOptionalFieldsWhenOverwriteNullsIsFalse() {
        IngredientStock existing = ingredient("ING0001", "Old Name", "Spice");
        existing.setDescription("Keep me");
        existing.setTotalStock(1.0);
        existing.setReorderLevel(2.0);
        existing.setUnitOfMeasure("KG");
        existing.setIsActive(true);

        when(ingredientStockRepository.findAllBySkuIn(anyCollection()))
                .thenReturn(List.of(existing), List.of(existing));
        when(ingredientStockRepository.saveAll(anyCollection()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IngredientImportPreviewResponse preview = ingredientImportService.previewImport(csvFile("""
                sku,ingredient_name,description,restaurant_id,total_stock,unit_code,category,reorder_level
                ING0001,Updated Name,,1,,KG,, 
                ING0002,Fresh Curd,Daily dairy,1,10.0,KG,Dairy,1.5
                """), 1L, false);

        IngredientImportCommitResponse commit = ingredientImportService.commitImport(preview.previewToken());

        assertThat(commit.createdCount()).isEqualTo(1);
        assertThat(commit.updatedCount()).isEqualTo(1);
        assertThat(commit.skippedOrErrorCount()).isZero();
        assertThat(commit.availabilityRefreshTriggered()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<IngredientStock>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(ingredientStockRepository).saveAll(captor.capture());
        List<IngredientStock> savedRows = List.copyOf(captor.getValue());
        assertThat(savedRows).hasSize(2);

        IngredientStock updated = savedRows.stream().filter(row -> "ING0001".equals(row.getSku())).findFirst().orElseThrow();
        IngredientStock created = savedRows.stream().filter(row -> "ING0002".equals(row.getSku())).findFirst().orElseThrow();

        assertThat(updated.getIngredientName()).isEqualTo("Updated Name");
        assertThat(updated.getDescription()).isEqualTo("Keep me");
        assertThat(updated.getCategory()).isEqualTo("Spice");
        assertThat(updated.getTotalStock()).isEqualTo(1.0);
        assertThat(updated.getReorderLevel()).isEqualTo(2.0);

        assertThat(created.getIngredientName()).isEqualTo("Fresh Curd");
        assertThat(created.getCategory()).isEqualTo("Dairy");
        assertThat(created.getDescription()).isEqualTo("Daily dairy");
        assertThat(created.getTotalStock()).isEqualTo(10.0);
        assertThat(created.getReorderLevel()).isEqualTo(1.5);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> skuCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(inventoryService, times(1)).refreshMenuAvailability(eq(List.of()), skuCaptor.capture());
        assertThat(skuCaptor.getValue()).containsExactly("ING0001", "ING0002");
    }

    @Test
    void previewImportRejectsQuantityChangesForExistingIngredients() {
        IngredientStock existing = ingredient("ING0001", "Turmeric Powder", "Spice");
        existing.setTotalStock(3.0);
        when(ingredientStockRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(existing));

        IngredientImportPreviewResponse response = ingredientImportService.previewImport(csvFile("""
                sku,ingredient_name,description,restaurant_id,total_stock,unit_code,category
                ING0001,Turmeric Powder,Updated,1,2.0,KG,Spice
                """), 1L, false);

        assertThat(response.validRowCount()).isZero();
        assertThat(response.invalidRowCount()).isEqualTo(1);
        assertThat(response.rows().get(0).errors()).contains("total_stock cannot be updated for existing ingredients");
    }

    private IngredientStock ingredient(String sku, String ingredientName, String category) {
        IngredientStock ingredient = new IngredientStock();
        ingredient.setSku(sku);
        ingredient.setRestaurantId(1L);
        ingredient.setIngredientName(ingredientName);
        ingredient.setCategory(category);
        ingredient.setTotalStock(0.0);
        ingredient.setUnitOfMeasure("KG");
        ingredient.setIsActive(true);
        ingredient.setIsDeleted(false);
        return ingredient;
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "ingredients.csv",
                "text/csv",
                content.getBytes()
        );
    }
}
