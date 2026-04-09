package com.kritik.POS.admin.service.Impl;

import com.kritik.POS.admin.models.response.ShortReport;
import com.kritik.POS.admin.views.projection.MostOrderedMenuProjection;
import com.kritik.POS.order.model.response.PaymentByHour;
import com.kritik.POS.order.model.response.PaymentByHourProjection;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPaymentServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private AdminPaymentServiceImpl adminPaymentService;

    @Test
    void getShortReportUsesDirectOrderAggregation() {
        LocalDate reportDate = LocalDate.of(2026, 3, 28);
        List<Long> restaurantIds = List.of(10L, 20L);

        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(restaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(restaurantIds)).thenReturn(restaurantIds);
        when(orderRepository.countByPaymentStatusForReportWindow(any(), eq(false), eq(restaurantIds), any(), any()))
                .thenReturn(6L, 2L);
        when(orderRepository.getTotalAmountByPaymentStatusForReportWindow(any(), any(), eq(false), eq(restaurantIds), any()))
                .thenReturn(2400.0);
        when(orderRepository.findAverageOrderValueByPaymentStatusForReportWindow(any(), any(), eq(false), eq(restaurantIds), any()))
                .thenReturn(400.0);
        when(orderRepository.findLatestOrderAmountsByPaymentStatusForReportWindow(any(), eq(false), eq(restaurantIds), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(455.0));

        ShortReport report = adminPaymentService.getShortReport(reportDate);

        assertThat(report.getTotalOrderCount()).isEqualTo(6L);
        assertThat(report.getCancelCount()).isEqualTo(2L);
        assertThat(report.getLastOrderAmount()).isEqualTo(455.0);
        assertThat(report.getTotalAmountEarned()).isEqualTo(2400.0);
        assertThat(report.getAverageOrderValue()).isEqualTo(400.0);
        assertThat(report.getReportDate()).isEqualTo(reportDate);
    }

    @Test
    void getHourlyPaymentReportFillsMissingHoursWithZeroes() {
        List<Long> restaurantIds = List.of(5L);
        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(restaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(restaurantIds)).thenReturn(restaurantIds);
        when(orderRepository.countPaymentsByHour(eq(2), eq(false), eq(restaurantIds), any(), any()))
                .thenReturn(List.of(
                        hourlyPayment(9, 3L, 450.0),
                        hourlyPayment(14, 7L, 910.0)
                ));

        List<PaymentByHour> report = adminPaymentService.getHourlyPaymentReport();

        assertThat(report).hasSize(24);
        assertThat(report.get(9).getNumberOfPayments()).isEqualTo(3L);
        assertThat(report.get(9).getTotalRevenue()).isEqualTo(450.0);
        assertThat(report.get(14).getNumberOfPayments()).isEqualTo(7L);
        assertThat(report.get(0).getNumberOfPayments()).isZero();
        assertThat(report.get(23).getNumberOfPayments()).isZero();
    }

    @Test
    void getMostOrderedItemUsesDirectSaleItemAggregation() {
        List<Long> restaurantIds = List.of(9L);
        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(restaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(restaurantIds)).thenReturn(restaurantIds);
        when(saleItemRepository.findMostOrderedItemsByPaymentStatusAndDate(any(), eq(false), eq(restaurantIds), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(mostOrderedMenu("Burger", 12L, 1440.0)));

        List<MostOrderedMenuProjection> report = adminPaymentService.getMostOrderedItem(7, 5);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).getSaleItemName()).isEqualTo("Burger");
        assertThat(report.get(0).getTotalQuantity()).isEqualTo(12L);
        assertThat(report.get(0).getTotalRevenue()).isEqualTo(1440.0);
    }

    @Test
    void getShortReportReturnsZeroesWhenNoRestaurantAccess() {
        LocalDate reportDate = LocalDate.of(2026, 4, 5);
        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(List.of());
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);

        ShortReport report = adminPaymentService.getShortReport(reportDate);

        assertThat(report.getTotalOrderCount()).isZero();
        assertThat(report.getCancelCount()).isZero();
        assertThat(report.getLastOrderAmount()).isZero();
        assertThat(report.getTotalAmountEarned()).isZero();
        assertThat(report.getAverageOrderValue()).isZero();
        assertThat(report.getReportDate()).isEqualTo(reportDate);
    }

    @Test
    void getMostOrderedItemReturnsEmptyListWhenNoRestaurantAccess() {
        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(List.of());
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(List.of())).thenReturn(List.of());
        when(saleItemRepository.findMostOrderedItemsByPaymentStatusAndDate(any(), eq(false), eq(List.of()), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        List<MostOrderedMenuProjection> report = adminPaymentService.getMostOrderedItem(7, 5);

        assertThat(report).isEmpty();
    }

    private PaymentByHourProjection hourlyPayment(Integer hourOfDay, Long totalOrders, Double totalRevenue) {
        return new PaymentByHourProjection() {
            @Override
            public Integer getHourOfDay() {
                return hourOfDay;
            }

            @Override
            public Long getNumberOfPayments() {
                return totalOrders;
            }

            @Override
            public Double getTotalRevenue() {
                return totalRevenue;
            }
        };
    }

    private MostOrderedMenuProjection mostOrderedMenu(String saleItemName, Long totalQuantity, Double totalRevenue) {
        return new MostOrderedMenuProjection() {
            @Override
            public String getSaleItemName() {
                return saleItemName;
            }

            @Override
            public Long getTotalQuantity() {
                return totalQuantity;
            }

            @Override
            public Double getTotalRevenue() {
                return totalRevenue;
            }
        };
    }
}
