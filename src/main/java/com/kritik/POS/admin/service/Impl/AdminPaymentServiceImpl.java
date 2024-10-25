package com.kritik.POS.admin.service.Impl;

import com.kritik.POS.admin.models.response.LastOrderListItem;
import com.kritik.POS.admin.models.response.MostOrderedMenu;
import com.kritik.POS.admin.models.response.OrderResponse;
import com.kritik.POS.admin.models.response.ShortReport;
import com.kritik.POS.admin.service.AdminPaymentService;
import com.kritik.POS.common.util.StoreUtil;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.order.DAO.Order;
import com.kritik.POS.order.DAO.enums.PaymentStatus;
import com.kritik.POS.order.DAO.enums.PaymentType;
import com.kritik.POS.order.model.response.PaymentByHour;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import org.attoparser.util.TextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminPaymentServiceImpl implements AdminPaymentService {
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final SaleItemRepository saleItemRepository;

    @Autowired
    public AdminPaymentServiceImpl(OrderRepository orderRepository, MenuItemRepository menuItemRepository, SaleItemRepository saleItemRepository) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.saleItemRepository = saleItemRepository;
    }

    @Override
    public List<OrderResponse> getAllOrdersToday(PaymentStatus paymentStatus, PaymentType paymentType, String orderId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        return orderRepository.findAllByPaymentTypeAndPaymentStatusAndLastUpdatedTimeBetweenOrderByLastUpdatedTimeDesc
                        (paymentType, paymentStatus,
                                startDate.atTime(StoreUtil.STORE_OPEN_TIME), endDate.atTime(StoreUtil.STORE_CLOSE_TIME))
                .stream().map(OrderResponse::buildObjectFromOrder).toList();

    }

    @Override
    public ShortReport getShortReport(LocalDate localDate) {
        Double avrageOrderValue = orderRepository.findAverageOrderValueByDateAndPaymentType(
                localDate.atTime(StoreUtil.STORE_OPEN_TIME),
                localDate.atTime(StoreUtil.STORE_CLOSE_TIME),
                PaymentStatus.PAYMENT_SUCCESSFUL);
        if (avrageOrderValue == null) {
            avrageOrderValue = 0.0;
        }

        Double totalOrderValue = orderRepository.getTotalAmountByLastUpdatedTimeAndPaymentStatus(
                localDate.atTime(StoreUtil.STORE_OPEN_TIME),
                localDate.atTime(StoreUtil.STORE_CLOSE_TIME),
                PaymentStatus.PAYMENT_SUCCESSFUL);
        if (totalOrderValue == null) {
            totalOrderValue = 0.0;
        }

        long successCount = orderRepository.countByPaymentStatusAndLastUpdatedTimeBetween(
                PaymentStatus.PAYMENT_SUCCESSFUL,
                localDate.atTime(StoreUtil.STORE_OPEN_TIME),
                localDate.atTime(StoreUtil.STORE_CLOSE_TIME));
        long refundCount = orderRepository.countByPaymentStatusAndLastUpdatedTimeBetween(
                PaymentStatus.PAYMENT_REFUND,
                localDate.atTime(StoreUtil.STORE_OPEN_TIME),
                localDate.atTime(StoreUtil.STORE_CLOSE_TIME));
        List<Order> lastOrders = orderRepository.findLastOrders(PaymentStatus.PAYMENT_SUCCESSFUL, PageRequest.of(0, 1));
        double lastOrderValue = 0.0;
        if (!lastOrders.isEmpty()) {
            lastOrderValue = lastOrders.stream().findFirst().orElseThrow(() -> new AppException("Internal Server error", HttpStatus.INTERNAL_SERVER_ERROR)).getTotalPrice();
        }
        return new ShortReport(successCount, refundCount, lastOrderValue, totalOrderValue, avrageOrderValue, LocalDate.now());
    }

    @Override
    public List<PaymentByHour> getHourlyPaymentReport() {
        List<PaymentByHour> paymentByHours = orderRepository.countPaymentsByHour(PaymentStatus.PAYMENT_SUCCESSFUL, LocalDate.now());
        Map<Integer, PaymentByHour> paymentByHourMap = paymentByHours.stream()
                .collect(Collectors.toMap(PaymentByHour::getHourOfDay, p -> p));
        List<PaymentByHour> response = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            PaymentByHour paymentByHour = paymentByHourMap.getOrDefault(i, new PaymentByHour(i, 0L));
            response.add(paymentByHour);
        }
        return response;
    }

    @Override
    public List<LastOrderListItem> getLast5Payments() {
        List<Order> lastOrders = orderRepository.findLastOrders(PaymentStatus.PAYMENT_SUCCESSFUL,
                PageRequest.of(0, 5));
        return lastOrders.stream().map(LastOrderListItem::new).toList();
    }

    @Override
    public List<MostOrderedMenu> getMostOrderedItem(Integer lastDays, Integer limit) {
        List<MostOrderedMenu> mostOrderedItemsByPaymentStatusAndDate =
                saleItemRepository.findMostOrderedItemsByPaymentStatusAndDate(PaymentStatus.PAYMENT_SUCCESSFUL,
                        LocalDate.now().minusDays(lastDays).atTime(StoreUtil.STORE_OPEN_TIME),
                        LocalDate.now().atTime(StoreUtil.STORE_CLOSE_TIME),
                        PageRequest.of(0, limit));
        return mostOrderedItemsByPaymentStatusAndDate;
    }


}
