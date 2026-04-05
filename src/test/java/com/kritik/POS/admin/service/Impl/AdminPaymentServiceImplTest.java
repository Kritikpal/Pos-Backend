package com.kritik.POS.admin.service.Impl;

import com.kritik.POS.admin.models.response.ShortReport;
import com.kritik.POS.admin.views.entity.HourlyPaymentView;
import com.kritik.POS.admin.views.repository.DailyKpiRepository;
import com.kritik.POS.admin.views.repository.HourlyPaymentViewRepository;
import com.kritik.POS.admin.views.repository.MostOrderedMenuRepository;
import com.kritik.POS.order.model.response.PaymentByHour;
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
import java.util.Collections;
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

    @Mock
    private HourlyPaymentViewRepository hourlyPaymentViewRepository;

    @Mock
    private DailyKpiRepository dailyKpiRepository;

    @Mock
    private MostOrderedMenuRepository mostOrderedMenuRepository;

    @InjectMocks
    private AdminPaymentServiceImpl adminPaymentService;

    @Test
    void getShortReportUsesMaterializedKpiAggregation() {
        LocalDate reportDate = LocalDate.of(2026, 3, 28);
        List<Long> restaurantIds = List.of(10L, 20L);

        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(restaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(restaurantIds)).thenReturn(restaurantIds);
        when(dailyKpiRepository.getAggregatedDailyKpi(false, restaurantIds, reportDate))
                .thenReturn(Collections.singletonList(new Object[]{6L, 2L, 2400.0, 400.0}));
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
        when(hourlyPaymentViewRepository.findHourlyPayments(eq(2), eq(false), eq(restaurantIds), any()))
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

    private HourlyPaymentView hourlyPayment(Integer hourOfDay, Long totalOrders, Double totalRevenue) {
        HourlyPaymentView view = new HourlyPaymentView();
        view.setHourOfDay(hourOfDay);
        view.setTotalOrders(totalOrders);
        view.setTotalRevenue(totalRevenue);
        return view;
    }
}
