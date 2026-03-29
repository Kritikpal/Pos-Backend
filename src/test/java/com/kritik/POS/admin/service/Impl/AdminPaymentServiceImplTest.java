package com.kritik.POS.admin.service.Impl;

import com.kritik.POS.admin.models.response.ShortReport;
import com.kritik.POS.order.entity.enums.PaymentStatus;
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
import static org.mockito.Mockito.verify;
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
    void getShortReportUsesSuccessfulAndRefundStatusesForSummary() {
        LocalDate reportDate = LocalDate.of(2026, 3, 28);
        List<Long> restaurantIds = List.of(10L, 20L);

        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(restaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(restaurantIds)).thenReturn(restaurantIds);
        when(orderRepository.countByPaymentStatusForReportWindow(eq(PaymentStatus.PAYMENT_SUCCESSFUL), eq(false), eq(restaurantIds), any(), any()))
                .thenReturn(6L);
        when(orderRepository.countByPaymentStatusForReportWindow(eq(PaymentStatus.PAYMENT_REFUND), eq(false), eq(restaurantIds), any(), any()))
                .thenReturn(2L);
        when(orderRepository.findLatestOrderAmountsByPaymentStatusForReportWindow(eq(PaymentStatus.PAYMENT_SUCCESSFUL), eq(false), eq(restaurantIds), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(455.0));
        when(orderRepository.getTotalAmountByPaymentStatusForReportWindow(any(), any(), eq(false), eq(restaurantIds), eq(PaymentStatus.PAYMENT_SUCCESSFUL)))
                .thenReturn(2400.0);
        when(orderRepository.findAverageOrderValueByPaymentStatusForReportWindow(any(), any(), eq(false), eq(restaurantIds), eq(PaymentStatus.PAYMENT_SUCCESSFUL)))
                .thenReturn(400.0);

        ShortReport report = adminPaymentService.getShortReport(reportDate);

        assertThat(report.getTotalOrderCount()).isEqualTo(6L);
        assertThat(report.getCancelCount()).isEqualTo(2L);
        assertThat(report.getLastOrderAmount()).isEqualTo(455.0);
        assertThat(report.getTotalAmountEarned()).isEqualTo(2400.0);
        assertThat(report.getAverageOrderValue()).isEqualTo(400.0);
        assertThat(report.getReportDate()).isEqualTo(reportDate);
        verify(orderRepository).countByPaymentStatusForReportWindow(eq(PaymentStatus.PAYMENT_REFUND), eq(false), eq(restaurantIds), any(), any());
    }

    @Test
    void getHourlyPaymentReportFillsMissingHoursWithZeroes() {
        List<Long> restaurantIds = List.of(5L);
        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(restaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(restaurantIds)).thenReturn(restaurantIds);
        when(orderRepository.countPaymentsByHour(eq(PaymentStatus.PAYMENT_SUCCESSFUL.ordinal()), eq(false), eq(restaurantIds), any(), any()))
                .thenReturn(List.of(
                        projection(9, 3L),
                        projection(14, 7L)
                ));

        List<PaymentByHour> report = adminPaymentService.getHourlyPaymentReport();

        assertThat(report).hasSize(24);
        assertThat(report.get(9).getNumberOfPayments()).isEqualTo(3L);
        assertThat(report.get(14).getNumberOfPayments()).isEqualTo(7L);
        assertThat(report.get(0).getNumberOfPayments()).isZero();
        assertThat(report.get(23).getNumberOfPayments()).isZero();
    }

    private PaymentByHourProjection projection(Integer hourOfDay, Long numberOfPayments) {
        return new PaymentByHourProjection() {
            @Override
            public Integer getHourOfDay() {
                return hourOfDay;
            }

            @Override
            public Long getNumberOfPayments() {
                return numberOfPayments;
            }
        };
    }
}
