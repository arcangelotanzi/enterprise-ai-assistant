package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.customer.Customer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record LateOrderResponse(
        Long orderId,
        Long customerId,
        String customerName,
        String customerEmail,
        String customerPhone,
        OrderStatus status,
        LocalDate promisedDeliveryDate,
        long daysLate,
        BigDecimal totalAmount) {

    public static LateOrderResponse from(Order order, LocalDate today) {
        Customer customer = order.getCustomer();
        return new LateOrderResponse(
                order.getId(),
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                order.getStatus(),
                order.getPromisedDeliveryDate(),
                ChronoUnit.DAYS.between(order.getPromisedDeliveryDate(), today),
                order.getTotalAmount());
    }
}
