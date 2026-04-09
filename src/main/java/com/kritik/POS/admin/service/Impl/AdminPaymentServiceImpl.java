package com.kritik.POS.admin.service.Impl;

import com.kritik.POS.admin.models.response.OrderResponse;
import com.kritik.POS.admin.models.response.ShortReport;
import com.kritik.POS.admin.service.AdminPaymentService;
import com.kritik.POS.admin.views.projection.MostOrderedMenuProjection;
import com.kritik.POS.common.util.StoreUtil;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.response.LastOrderListItemProjection;
import com.kritik.POS.order.model.response.PaymentByHour;
import com.kritik.POS.order.model.response.PaymentByHourProjection;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPaymentServiceImpl implements AdminPaymentService {
    private final OrderRepository orderRepository;
    private final SaleItemRepository saleItemRepository;
    private final TenantAccessService tenantAccessService;

    @Override
    public List<OrderResponse> getAllOrdersToday(PaymentStatus paymentStatus, PaymentType paymentType, String orderId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        if (orderId == null || orderId.isBlank()) {
            orderId = null;
        }

        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }

        return orderRepository.findAllByFilters(
                        orderId,
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        paymentType,
                        paymentStatus,
                        startDate.atTime(StoreUtil.STORE_OPEN_TIME),
                        endDate.atTime(StoreUtil.STORE_CLOSE_TIME),
                        PageRequest.of(0, 100)
                )
                .stream()
                .map(OrderResponse::buildObjectFromOrder)
                .toList();

    }

    @Override
    public ShortReport getShortReport(LocalDate localDate) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new ShortReport(0L, 0L, 0.0, 0.0, 0.0, localDate);
        }

        ReportWindow reportWindow = buildReportWindow(localDate);
        boolean skipRestaurantFilter = tenantAccessService.isSuperAdmin();
        List<Long> restaurantIds = tenantAccessService.queryRestaurantIds(accessibleRestaurantIds);

        long successCount = orderRepository.countByPaymentStatusForReportWindow(
                PaymentStatus.PAYMENT_SUCCESSFUL,
                skipRestaurantFilter,
                restaurantIds,
                reportWindow.start(),
                reportWindow.endExclusive()
        );
        long cancelCount = orderRepository.countByPaymentStatusForReportWindow(
                PaymentStatus.PAYMENT_CANCELED,
                skipRestaurantFilter,
                restaurantIds,
                reportWindow.start(),
                reportWindow.endExclusive()
        );
        double totalRevenue = defaultDouble(orderRepository.getTotalAmountByPaymentStatusForReportWindow(
                reportWindow.start(),
                reportWindow.endExclusive(),
                skipRestaurantFilter,
                restaurantIds,
                PaymentStatus.PAYMENT_SUCCESSFUL
        ));
        double avgOrderValue = defaultDouble(orderRepository.findAverageOrderValueByPaymentStatusForReportWindow(
                reportWindow.start(),
                reportWindow.endExclusive(),
                skipRestaurantFilter,
                restaurantIds,
                PaymentStatus.PAYMENT_SUCCESSFUL
        ));

        double lastOrderValue = orderRepository.findLatestOrderAmountsByPaymentStatusForReportWindow(
                        PaymentStatus.PAYMENT_SUCCESSFUL,
                        skipRestaurantFilter,
                        restaurantIds,
                        reportWindow.start(),
                        reportWindow.endExclusive(),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(0.0);


        return new ShortReport(successCount, cancelCount, lastOrderValue, totalRevenue, avgOrderValue, localDate);
    }

    @Override
    public List<PaymentByHour> getHourlyPaymentReport() {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return buildHourlyResponse(List.of());
        }

        List<PaymentByHourProjection> data = orderRepository.countPaymentsByHour(
                PaymentStatus.PAYMENT_SUCCESSFUL.ordinal(),
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                LocalDate.now().atTime(StoreUtil.STORE_OPEN_TIME),
                LocalDate.now().plusDays(1).atTime(StoreUtil.STORE_OPEN_TIME)
        );
        return buildHourlyResponse(data);
    }


    @Override
    public List<LastOrderListItemProjection> getLast5Payments() {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        return orderRepository.findLastOrders(
                PaymentStatus.PAYMENT_SUCCESSFUL.ordinal(),
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                PageRequest.of(0, 5)
        );
    }

    @Override
    public List<MostOrderedMenuProjection> getMostOrderedItem(Integer lastDays, Integer limit) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        return saleItemRepository.findMostOrderedItemsByPaymentStatusAndDate(
                PaymentStatus.PAYMENT_SUCCESSFUL,
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                LocalDate.now().minusDays(lastDays == null ? 0 : lastDays).atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay(),
                PageRequest.of(0, limit)
        );
    }

    private List<PaymentByHour> buildHourlyResponse(List<PaymentByHourProjection> data) {
        Map<Integer, PaymentByHour> map = data.stream()
                .collect(Collectors.toMap(
                        PaymentByHourProjection::getHourOfDay,
                        projection -> new PaymentByHour(
                                projection.getHourOfDay(),
                                projection.getNumberOfPayments(),
                                defaultDouble(projection.getTotalRevenue())
                        )
                ));

        List<PaymentByHour> response = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            response.add(map.getOrDefault(i, new PaymentByHour(i, 0L, 0.0)));
        }
        return response;
    }

    private double defaultDouble(Double value) {
        return value == null ? 0.0 : value;
    }


    private ReportWindow buildReportWindow(LocalDate reportDate) {
        LocalDateTime start = reportDate.atTime(StoreUtil.STORE_OPEN_TIME);
        LocalDateTime endExclusive = reportDate.plusDays(1).atTime(StoreUtil.STORE_OPEN_TIME);
        return new ReportWindow(start, endExclusive);
    }

    private record ReportWindow(LocalDateTime start, LocalDateTime endExclusive) {
    }
}
