package com.kritik.POS.inventory.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.response.StockReceiptResponseDto;
import com.kritik.POS.inventory.route.InventoryRoute;
import com.kritik.POS.inventory.service.ReceiptService;
import com.kritik.POS.restaurant.models.request.StockReceiptCreateRequest;
import com.kritik.POS.restaurant.models.response.StockReceiptResponse;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(InventoryRoute.BASE)
public class StockReceiptController {
    private final ReceiptService receiptService;

    @Tag(name = SwaggerTags.STOCK_RECEIPT)
    @GetMapping(InventoryRoute.GET_RECEIPTS_PAGE)
    public ResponseEntity<ApiResponse<PageResponse<StockReceiptResponseDto>>> receiptPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                receiptService.getReceiptPage(chainId, restaurantId, search, page, size)
        ));
    }

    @Tag(name = SwaggerTags.STOCK_RECEIPT)
    @GetMapping(InventoryRoute.GET_RECEIPT)
    public ResponseEntity<ApiResponse<StockReceiptResponse>> getReceipt(@PathVariable(name = "id") Long receiptId) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(receiptService.getReceiptById(receiptId)));
    }

    @Tag(name = SwaggerTags.STOCK_RECEIPT)
    @PostMapping(InventoryRoute.CREATE_RECEIPT)
    public ResponseEntity<ApiResponse<StockReceiptResponse>> createReceipt(
            @RequestBody @Valid StockReceiptCreateRequest stockReceiptCreateRequest
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                receiptService.createStockReceipt(stockReceiptCreateRequest),
                "Stock receipt created successfully"
        ));
    }
}
