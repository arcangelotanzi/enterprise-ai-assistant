package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.customer.Customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        OrderStatus status,
        LocalDate orderDate,
        LocalDate promisedDeliveryDate,
        Instant deliveredAt,
        BigDecimal totalAmount,
        Instant createdAt) {

    public static OrderResponse from(Order order) {
        Customer customer = order.getCustomer();
        return new OrderResponse(order.getId(),
                customer.getId(),
                customer.getName(),
                order.getStatus(),
                order.getOrderDate(),
                order.getPromisedDeliveryDate(),
                order.getDeliveredAt(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }
}
