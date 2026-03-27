package com.kritik.POS.admin.controller;

import static com.kritik.POS.admin.route.AdminRoute.GET_ENDING_STOCK;
import static com.kritik.POS.admin.route.AdminRoute.GET_LAST5_PAYMENTS;
import static com.kritik.POS.admin.route.AdminRoute.MOST_ORDERED_ITEM;

import com.kritik.POS.admin.models.response.LastOrderListItem;
import com.kritik.POS.admin.models.response.MostOrderedMenu;
import com.kritik.POS.admin.models.response.OrderResponse;
import com.kritik.POS.admin.models.response.ShortReport;
import com.kritik.POS.admin.route.AdminRoute;
import com.kritik.POS.admin.service.AdminPaymentService;
import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.response.PaymentByHour;
import com.kritik.POS.restaurant.models.response.StockReport;
import com.kritik.POS.restaurant.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Admin Payment")
public class AdminController {

    private final AdminPaymentService adminPaymentService;
    private final StockService stockService;

    public AdminController(AdminPaymentService adminPaymentService, StockService stockService) {
        this.adminPaymentService = adminPaymentService;
        this.stockService = stockService;
    }

    @GetMapping(AdminRoute.GET_ALL_ORDERS_TODAY)
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(name = "paymentStatus", required = false) PaymentStatus paymentStatus,
            @RequestParam(name = "paymentType", required = false) PaymentType paymentType,
            @RequestParam(name = "orderId", required = false) String orderId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<OrderResponse> allOrders = adminPaymentService.getAllOrdersToday(paymentStatus, paymentType, orderId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.SUCCESS(allOrders));
    }

    @GetMapping(AdminRoute.GET_TODAY_REPORT)
    public ResponseEntity<ApiResponse<ShortReport>> getTodaysReport() {
        ShortReport todaysReport = adminPaymentService.getShortReport(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.SUCCESS(todaysReport));
    }

    @GetMapping(AdminRoute.GET_HOURLY_REPORT)
    public ResponseEntity<ApiResponse<List<PaymentByHour>>> getHourlyReport() {
        List<PaymentByHour> todaysReport = adminPaymentService.getHourlyPaymentReport();
        return ResponseEntity.ok(ApiResponse.SUCCESS(todaysReport));
    }

    @GetMapping(GET_LAST5_PAYMENTS)
    public ResponseEntity<ApiResponse<List<LastOrderListItem>>> getLat5Payments() {
        List<LastOrderListItem> last5Payments = adminPaymentService.getLast5Payments();
        return ResponseEntity.ok(ApiResponse.SUCCESS(last5Payments));
    }

    @GetMapping(GET_ENDING_STOCK)
    public ResponseEntity<ApiResponse<List<StockReport>>> getEndingStocks(
            @RequestParam(name = "limit", defaultValue = "5") @Min(value = 1, message = "limit must be at least 1") @Max(value = 100, message = "limit must be 100 or less") Integer limit
    ) {
        List<StockReport> last5Payments = stockService.getALlStocks("", limit);
        return ResponseEntity.ok(ApiResponse.SUCCESS(last5Payments));
    }

    @GetMapping(MOST_ORDERED_ITEM)
    public ResponseEntity<ApiResponse<List<MostOrderedMenu>>> mostOrderedItem(
            @RequestParam(name = "lastDays", defaultValue = "7") @Min(value = 1, message = "lastDays must be at least 1") @Max(value = 365, message = "lastDays must be 365 or less") Integer lastDays,
            @RequestParam(name = "limit", defaultValue = "5") @Min(value = 1, message = "limit must be at least 1") @Max(value = 100, message = "limit must be 100 or less") Integer limit
    ) {
        List<MostOrderedMenu> mostOrderedItem = adminPaymentService.getMostOrderedItem(lastDays, limit);
        return ResponseEntity.ok(ApiResponse.SUCCESS(mostOrderedItem));
    }
}
