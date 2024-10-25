package com.kritik.POS.admin.service;

import com.kritik.POS.admin.models.response.LastOrderListItem;
import com.kritik.POS.admin.models.response.MostOrderedMenu;
import com.kritik.POS.admin.models.response.OrderResponse;
import com.kritik.POS.admin.models.response.ShortReport;
import com.kritik.POS.order.DAO.enums.PaymentStatus;
import com.kritik.POS.order.DAO.enums.PaymentType;
import com.kritik.POS.order.model.response.PaymentByHour;

import java.time.LocalDate;
import java.util.List;

public interface AdminPaymentService {

     List<OrderResponse> getAllOrdersToday(PaymentStatus statusFilters, PaymentType typeFilters, String orderId, LocalDate localDate, LocalDate endDate);
     ShortReport getShortReport(LocalDate now);
     List<PaymentByHour> getHourlyPaymentReport();
     List<LastOrderListItem> getLast5Payments();
     List<MostOrderedMenu> getMostOrderedItem(Integer lastDays, Integer limit);

}
