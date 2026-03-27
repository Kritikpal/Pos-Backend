package com.kritik.POS.admin.service.Impl;

import com.kritik.POS.admin.models.response.MostOrderedMenu;
import com.kritik.POS.admin.models.response.OrderResponse;
import com.kritik.POS.admin.models.response.ShortReport;
import com.kritik.POS.admin.service.AdminPaymentService;
import com.kritik.POS.common.util.StoreUtil;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.response.LastOrderListItemProjection;
import com.kritik.POS.order.model.response.PaymentByHour;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
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
        Double avrageOrderValue = orderRepository.findAverageOrderValueByDateAndPaymentType(
                localDate.atTime(StoreUtil.STORE_OPEN_TIME),
                localDate.atTime(StoreUtil.STORE_CLOSE_TIME),
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                PaymentStatus.PAYMENT_SUCCESSFUL
        );
        if (avrageOrderValue == null) {
            avrageOrderValue = 0.0;
        }

        Double totalOrderValue = orderRepository.getTotalAmountByLastUpdatedTimeAndPaymentStatus(
                localDate.atTime(StoreUtil.STORE_OPEN_TIME),
                localDate.atTime(StoreUtil.STORE_CLOSE_TIME),
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                PaymentStatus.PAYMENT_SUCCESSFUL
        );
        if (totalOrderValue == null) {
            totalOrderValue = 0.0;
        }

        long successCount = orderRepository.countByPaymentStatusAndLastUpdatedTimeBetween(
                PaymentStatus.PAYMENT_SUCCESSFUL,
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                localDate.atTime(StoreUtil.STORE_OPEN_TIME),
                localDate.atTime(StoreUtil.STORE_CLOSE_TIME)
        );
        long refundCount = orderRepository.countByPaymentStatusAndLastUpdatedTimeBetween(
                PaymentStatus.PAYMENT_REFUND,
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                localDate.atTime(StoreUtil.STORE_OPEN_TIME),
                localDate.atTime(StoreUtil.STORE_CLOSE_TIME)
        );
        List<LastOrderListItemProjection> lastOrders = orderRepository.findLastOrders(
                4,
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                PageRequest.of(0, 1)
        );
        double lastOrderValue = 0.0;
        if (!lastOrders.isEmpty()) {
            lastOrderValue = lastOrders.stream()
                    .findFirst()
                    .orElseThrow(() -> new AppException("Internal Server error", HttpStatus.INTERNAL_SERVER_ERROR))
                    .getTotalPrice();
        }
        return new ShortReport(successCount, refundCount, lastOrderValue, totalOrderValue, avrageOrderValue, LocalDate.now());
    }

    @Override
    public List<PaymentByHour> getHourlyPaymentReport() {

        List<Long> accessibleRestaurantIds =
                tenantAccessService.resolveAccessibleRestaurantIds(null, null);

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<PaymentByHour> paymentByHours = orderRepository.countPaymentsByHour(
                PaymentStatus.PAYMENT_SUCCESSFUL,
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                start,
                end
        );

        Map<Integer, PaymentByHour> paymentByHourMap = paymentByHours.stream()
                .collect(Collectors.toMap(PaymentByHour::getHourOfDay, p -> p));

        List<PaymentByHour> response = new ArrayList<>(24);

        for (int i = 0; i < 24; i++) {
            response.add(paymentByHourMap.getOrDefault(i, new PaymentByHour(i, 0L)));
        }

        return response;
    }

    @Override
    public List<LastOrderListItemProjection> getLast5Payments() {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        return orderRepository.findLastOrders(
                4,
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                PageRequest.of(0, 5)
        );
    }

    @Override
    public List<MostOrderedMenu> getMostOrderedItem(Integer lastDays, Integer limit) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        return saleItemRepository.findMostOrderedItemsByPaymentStatusAndDate(
                PaymentStatus.PAYMENT_SUCCESSFUL,
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                LocalDate.now().minusDays(lastDays).atTime(StoreUtil.STORE_OPEN_TIME),
                LocalDate.now().atTime(StoreUtil.STORE_CLOSE_TIME),
                PageRequest.of(0, limit)
        );
    }
}
